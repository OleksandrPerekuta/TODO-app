<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
  <title>Task Board</title>
  <style>
    .board {
      display: flex;
      justify-content: space-between;
    }
    .column {
      width: 30%;
      padding: 10px;
      border: 1px solid #ccc;
      border-radius: 4px;
      background-color: #f9f9f9;
    }
    .task {
      margin-bottom: 10px;
      padding: 10px;
      border: 1px solid #ccc;
      border-radius: 4px;
      background-color: #fff;
      cursor: move;
    }
    .column h2 {
      text-align: center;
    }
  </style>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/Sortable/1.14.0/Sortable.min.js"></script>
</head>
<body>
<h1>Task Board</h1>
<div class="board">
  <c:forEach var="entry" items="${tasksByLabel}">
    <div class="column" id="${entry.key}">
      <h2>${entry.key}</h2>
      <div class="tasks">
        <c:forEach var="task" items="${entry.value}">
          <div class="task" data-id="${task.id}">
            <h3>${task.name}</h3>
            <p>${task.description}</p>
          </div>
        </c:forEach>
      </div>
    </div>
  </c:forEach>
</div>
<script>
  document.addEventListener('DOMContentLoaded', function() {
    const columns = document.querySelectorAll('.column');

    columns.forEach(column => {
      new Sortable(column.querySelector('.tasks'), {
        group: 'tasks',
        animation: 150,
        handle: '.task',
        onEnd: function (evt) {
          let taskId = evt.item.getAttribute('data-id');
          let newLabel = evt.to.closest('.column').getAttribute('id');
          let newPosition = Array.from(evt.to.children).indexOf(evt.item);
          let body = 'id=' + taskId + '&label=' + newLabel + '&position=' + newPosition;
          fetch('http://localhost:8080/?' + body, {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: body
          })
                  .then(response => {
                    if (response.ok) {
                      console.log('Task updated successfully');
                    } else {
                      console.error('Error updating task');
                    }
                  })
                  .catch(error => console.error('Error:', error));
        }
      });
    });
  });
</script>
</body>
</html>
