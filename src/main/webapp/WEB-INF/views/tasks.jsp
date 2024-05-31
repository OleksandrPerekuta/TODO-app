<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
  <title>Task Board</title>
  <style>
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 60px;
    }
    .board {
      background-color: #344C64;
      240750
      color: black;
      display: flex;
      justify-content: space-between;
      height: calc(100vh - 80px);
    }
    .column {
      width: 30%;
      padding: 10px;
      border: 1px solid #577B8D;
      border-radius: 4px;
      background-color: #577B8D;
    }
    .task {
      margin-bottom: 10px;
      padding: 10px;
      border: 1px solid #57A6A1;
      border-radius: 4px;
      background-color: #57A6A1;
      cursor: move;
      position: relative;
      word-wrap: break-word;
    }
    .task h3 {
      margin: 0 0 10px;
    }
    .task p {
      margin: 0 0 10px;
    }
    .tasks {
      flex-grow: 1;
      overflow-y: auto;
    }
    .deleteButton {
      position: absolute;
      top: 10px;
      right: 10px;
      background-color: red;
      color: white;
      border: none;
      padding: 5px 10px;
      cursor: pointer;
      border-radius: 4px;
    }
    .column h2 {
      text-align: center;
    }
    .addButton {
      background-color: #344C64;
      color: white;
      border: none;
      padding: 20px 40px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 24px;
      margin-right: 40px;
    }
    body {
      color: white;
      background-color: #240750;
      margin-top: 20px;
    }
    .modal {
      display: none;
      position: fixed;
      z-index: 1;
      left: 0;
      top: 0;
      width: 100%;
      height: 100%;
      overflow: auto;
      background-color: rgba(0, 0, 0, 0.4);
    }
    .modal-content {
      background-color:#240750;
      color: white;
      margin: 15% auto;
      padding: 20px;
      border: 1px solid #888;
      width: 80%;
      max-width: 500px;
      border-radius: 4px;
    }
    .close {
      color: #aaa;
      float: right;
      font-size: 28px;
      font-weight: bold;
    }
    .close:hover,
    .close:focus {
      color: black;
      text-decoration: none;
      cursor: pointer;
    }
    .form-group {
      margin-bottom: 15px;
    }
    .form-group label {
      display: block;
      margin-bottom: 5px;
    }
    .form-group input,
    .form-group select {
      width: 100%;
      padding: 8px;
      box-sizing: border-box;
    }
  </style>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/Sortable/1.14.0/Sortable.min.js"></script>
</head>
<body>
<div class="header">
  <h1>Task Board</h1>
  <button class="addButton">Add+</button>
</div>

<div class="board">
  <c:forEach var="entry" items="${tasksByLabel}">
    <div class="column" id="${entry.key}">
      <h2>${entry.key}</h2>
      <div class="tasks">
        <c:forEach var="task" items="${entry.value}">
          <div class="task" data-id="${task.id}">
            <h3>${task.name}</h3>
            <p>${task.description}</p>
            <button class="deleteButton" data-id="${task.id}">Delete</button>
          </div>
        </c:forEach>
      </div>
    </div>
  </c:forEach>
</div>

<!-- The Modal -->
<div id="taskModal" class="modal">
  <div class="modal-content">
    <span class="close">&times;</span>
    <h2>Add New Task</h2>
    <div class="form-group">
      <label for="taskName">Name</label>
      <input type="text" id="taskName" name="taskName">
    </div>
    <div class="form-group">
      <label for="taskDescription">Description</label>
      <input type="text" id="taskDescription" name="taskDescription">
    </div>
    <div class="form-group">
      <label for="taskStatus">Status</label>
      <select id="taskStatus" name="taskStatus">
        <option value="To do">To do</option>
        <option value="In progress">In progress</option>
        <option value="Done">Done</option>
      </select>
    </div>
    <button id="saveTaskButton">Save</button>
  </div>
</div>
<script>
  document.addEventListener('DOMContentLoaded', function() {
    const columns = document.querySelectorAll('.column');
    const addButton = document.querySelector('.addButton');
    const modal = document.getElementById('taskModal');
    const closeModal = document.querySelector('.close');
    const saveTaskButton = document.getElementById('saveTaskButton');

    addButton.addEventListener('click', function() {
      modal.style.display = 'block';
    });

    closeModal.addEventListener('click', function() {
      modal.style.display = 'none';
    });

    window.addEventListener('click', function(event) {
      if (event.target === modal) {
        modal.style.display = 'none';
      }
    });

    saveTaskButton.addEventListener('click', function() {
      const taskName = document.getElementById('taskName').value;
      const taskDescription = document.getElementById('taskDescription').value;
      const taskStatus = document.getElementById('taskStatus').value;

      const body = new URLSearchParams({
        name: taskName,
        description: taskDescription,
        status: taskStatus
      });

      fetch('http://localhost:8080/?'+'name='+taskName+'&description='+taskDescription+'&status'+taskStatus, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body
      })
              .then(response => {
                if (response.ok) {
                  console.log('Task added successfully');
                  location.reload();
                } else {
                  console.error('Error adding task');
                }
              })
              .catch(error => console.error('Error:', error));

      modal.style.display = 'none';
    });

    columns.forEach(column => {
      new Sortable(column.querySelector('.tasks'), {
        group: 'tasks',
        animation: 150,
        handle: '.task',
        onEnd: function (evt) {
          let taskId = evt.item.getAttribute('data-id');
          let newLabel = evt.to.closest('.column').getAttribute('id');
          let newPosition = Array.from(evt.to.children).indexOf(evt.item);
          let oldLabel = evt.from.closest('.column').getAttribute('id');
          let body = 'id=' + taskId + '&label=' + newLabel + '&position=' + newPosition + '&oldLabel=' +oldLabel;

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

    document.querySelectorAll('.deleteButton').forEach(button => {
      button.addEventListener('click', function() {
        const taskId = this.getAttribute('data-id');
        fetch('http://localhost:8080/?id=' + taskId, {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        })
                .then(response => {
                  if (response.ok) {
                    console.log('Task deleted successfully');
                    this.parentElement.remove();
                  } else {
                    console.error('Error deleting task');
                  }
                })
                .catch(error => console.error('Error:', error));
      });
    });
  });
</script>
</body>
</html>
