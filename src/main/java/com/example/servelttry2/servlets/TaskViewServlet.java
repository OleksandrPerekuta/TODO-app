package com.example.servelttry2.servlets;

import com.example.servelttry2.model.Task;
import com.example.servelttry2.util.HibernateUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TaskViewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Map<String, List<Task>> tasksByLabel = new LinkedHashMap<>();
        for (String label : new String[]{"To do", "In progress", "Done"}) {
            List<Task> tasks;
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                String hql = "FROM Task WHERE status = :label order by position";
                Query<Task> query = session.createQuery(hql, Task.class);
                query.setParameter("label", label);
                tasks = query.getResultList();
                tasksByLabel.put(label, tasks);
            } catch (Exception e) {
                throw new ServletException(e.getMessage());
            }
        }

        request.setAttribute("tasksByLabel", tasksByLabel);
        request.getRequestDispatcher("/WEB-INF/views/tasks.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String status = request.getParameter("status");

        if (name == null || description == null || status == null) {
            throw new ServletException("Parameters must not be null");
        }
        if (!isCorrect(status)) {
            throw new ServletException("Status is incorrect");
        }

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String hqlUpdate = "UPDATE Task SET position = position + 1 WHERE status = :status";
            session.createQuery(hqlUpdate)
                    .setParameter("status", status)
                    .executeUpdate();

            Task task = new Task(name, description, status, 0L);
            session.save(task);

            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            response.getWriter().println("Error adding task!");
            return;
        }

        response.getWriter().println("Task added successfully!");
    }

    private boolean isCorrect(String task) {
        return task.equals("To do") || task.equals("In progress") || task.equals("Done");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        String newLabel = req.getParameter("label");
        String newPositionStr = req.getParameter("position");
        String oldLabel = req.getParameter("oldLabel");

        if (id == null || newLabel == null || newPositionStr == null || oldLabel == null || id.isEmpty() || newLabel.isEmpty() || newPositionStr.isEmpty() || oldLabel.isEmpty()) {
            throw new ServletException("Parameters must not be null or empty.");
        }

        Long taskId = Long.parseLong(id);
        Long newPosition = Long.parseLong(newPositionStr);

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String hqlSelect = "SELECT t.position FROM Task t WHERE t.id = :id";
            Long currentPosition = (Long) session.createQuery(hqlSelect)
                    .setParameter("id", taskId)
                    .uniqueResult();

            if (currentPosition == null) {
                throw new ServletException("Task not found");
            }

            String hqlUpdateOldColumn = "UPDATE Task t SET t.position = t.position - 1 WHERE t.status = :oldLabel AND t.position > :currentPosition";
            session.createQuery(hqlUpdateOldColumn)
                    .setParameter("oldLabel", oldLabel)
                    .setParameter("currentPosition", currentPosition)
                    .executeUpdate();

            String hqlUpdateNewColumn = "UPDATE Task t SET t.position = t.position + 1 WHERE t.status = :newLabel AND t.position >= :newPosition";
            session.createQuery(hqlUpdateNewColumn)
                    .setParameter("newLabel", newLabel)
                    .setParameter("newPosition", newPosition)
                    .executeUpdate();

            String hqlUpdateTask = "UPDATE Task t SET t.status = :newLabel, t.position = :newPosition WHERE t.id = :id";
            session.createQuery(hqlUpdateTask)
                    .setParameter("newLabel", newLabel)
                    .setParameter("newPosition", newPosition)
                    .setParameter("id", taskId)
                    .executeUpdate();

            transaction.commit();
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new ServletException(e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        if (id == null || id.isEmpty()) {
            throw new ServletException("Id must not be null or empty");
        }

        Long taskId = Long.parseLong(id);

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String hqlSelect = "SELECT t.status, t.position FROM Task t WHERE t.id = :id";
            Object[] result = (Object[]) session.createQuery(hqlSelect)
                    .setParameter("id", taskId)
                    .uniqueResult();

            if (result == null) {
                throw new ServletException("Task not found");
            }

            String status = (String) result[0];
            Long position = (Long) result[1];

            String hqlDelete = "DELETE FROM Task t WHERE t.id = :id";
            session.createQuery(hqlDelete)
                    .setParameter("id", taskId)
                    .executeUpdate();

            String hqlUpdatePositions = "UPDATE Task t SET t.position = t.position - 1 WHERE t.status = :status AND t.position > :position";
            session.createQuery(hqlUpdatePositions)
                    .setParameter("status", status)
                    .setParameter("position", position)
                    .executeUpdate();

            transaction.commit();
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new ServletException(e.getMessage());
        }
    }
}
