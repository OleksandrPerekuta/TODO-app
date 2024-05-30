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
        for (String label : new String[]{"to-do", "in-progress", "done"}) {
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
            int updatedEntities = session.createQuery(hqlUpdate)
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
        return task.equals("to-do") || task.equals("in-progress") || task.equals("done");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        String label = req.getParameter("label");
        String position = req.getParameter("position");

        if (id == null || label == null || position == null || id.isEmpty() || label.isEmpty() || position.isEmpty()) {
            boolean isIdEmpty = id == null || id.isEmpty();
            boolean isLabelEmpty = label == null || label.isEmpty();
            boolean isPositionEmpty = position == null || position.isEmpty();
            throw new ServletException("Parameters must not be null or empty. isIdEmpty= " + isIdEmpty + ", isLabelEmpty= " + isLabelEmpty + ", isPositionEmpty= " + isPositionEmpty);
        }

        Long taskId = Long.parseLong(id);
        Long newPosition = Long.parseLong(position);

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
            if (newPosition < currentPosition) {
                String hqlUpdatePositions = "UPDATE Task t SET t.position = t.position + 1 WHERE t.status = :label AND t.position >= :newPosition AND t.position < :currentPosition";
                session.createQuery(hqlUpdatePositions)
                        .setParameter("label", label)
                        .setParameter("newPosition", newPosition)
                        .setParameter("currentPosition", currentPosition)
                        .executeUpdate();
            } else if (newPosition > currentPosition) {
                String hqlUpdatePositions = "UPDATE Task t SET t.position = t.position - 1 WHERE t.status = :label AND t.position <= :newPosition AND t.position > :currentPosition";
                session.createQuery(hqlUpdatePositions)
                        .setParameter("label", label)
                        .setParameter("newPosition", newPosition)
                        .setParameter("currentPosition", currentPosition)
                        .executeUpdate();
            }
            String hqlUpdateTask = "UPDATE Task t SET t.status = :label, t.position = :newPosition WHERE t.id = :id";
            session.createQuery(hqlUpdateTask)
                    .setParameter("label", label)
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



}
