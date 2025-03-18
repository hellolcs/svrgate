// user.js - 계정관리 페이지 스크립트

// 전역 변수 (HTML에서 설정)
let MODAL_TYPE = null;
let MODAL_ERROR = null;
let SELECTED_USERNAME = null;

document.addEventListener('DOMContentLoaded', function() {
  // 전역 변수 초기화 (HTML에서 설정한 값 사용)
  MODAL_TYPE = document.getElementById('modalTypeData')?.value || null;
  MODAL_ERROR = document.getElementById('modalErrorData')?.value || null;
  SELECTED_USERNAME = document.getElementById('selectedUsernameData')?.value || null;
  
  // 계정 추가 버튼
  document.getElementById('addUserBtn')?.addEventListener('click', function() {
    var addModal = new bootstrap.Modal(document.getElementById('addUserModal'));
    addModal.show();
  });
  
  // 계정 수정 버튼
  document.querySelectorAll('.editUserBtn').forEach(function(button) {
    button.addEventListener('click', function() {
      // 현재 행 가져오기
      var row = this.closest('tr');
      
      // 폼에 데이터 설정
      document.getElementById('edit-username').value = row.getAttribute('data-username');
      document.getElementById('edit-name').value = row.getAttribute('data-name');
      document.getElementById('edit-department').value = row.getAttribute('data-department');
      document.getElementById('edit-phoneNumber').value = row.getAttribute('data-phone');
      document.getElementById('edit-email').value = row.getAttribute('data-email');
      document.getElementById('edit-allowedLoginIps').value = row.getAttribute('data-allowedips');
      
      // 비밀번호 필드는 비워두기
      document.getElementById('edit-password').value = '';
      document.getElementById('edit-passwordConfirm').value = '';
      
      // 모달 표시
      var editModal = new bootstrap.Modal(document.getElementById('editUserModal'));
      editModal.show();
    });
  });
  
  // 계정 삭제 버튼
  document.querySelectorAll('.deleteUserBtn').forEach(function(button) {
    button.addEventListener('click', function() {
      // 현재 행 가져오기
      var row = this.closest('tr');
      var username = row.getAttribute('data-username');
      
      if (confirm("정말로 사용자 '" + username + "'를 삭제하시겠습니까?")) {
        document.getElementById('deleteUserId').value = username;
        document.getElementById('deleteForm').submit();
      }
    });
  });
  
  // 모달 관련 오류 처리
  handleModalErrors();
});

/**
 * 모달 오류 처리 함수
 */
function handleModalErrors() {
  if (MODAL_TYPE && MODAL_ERROR) {
    if (MODAL_TYPE === 'add') {
      const addModalAlert = document.getElementById('addModalAlert');
      const addModalAlertText = document.getElementById('addModalAlertText');
      
      if (addModalAlert && addModalAlertText) {
        addModalAlertText.textContent = MODAL_ERROR;
        addModalAlert.style.display = 'block';
        
        var addModal = new bootstrap.Modal(document.getElementById('addUserModal'));
        addModal.show();
      }
    } else if (MODAL_TYPE === 'update') {
      const editModalAlert = document.getElementById('editModalAlert');
      const editModalAlertText = document.getElementById('editModalAlertText');
      
      if (editModalAlert && editModalAlertText) {
        editModalAlertText.textContent = MODAL_ERROR;
        editModalAlert.style.display = 'block';
        
        // 선택된 사용자가 있으면 처리
        if (SELECTED_USERNAME) {
          const row = document.querySelector(`tr[data-username="${SELECTED_USERNAME}"]`);
          if (row) {
            // 폼에 데이터 설정
            document.getElementById('edit-username').value = row.getAttribute('data-username');
            document.getElementById('edit-name').value = row.getAttribute('data-name');
            document.getElementById('edit-department').value = row.getAttribute('data-department');
            document.getElementById('edit-phoneNumber').value = row.getAttribute('data-phone');
            document.getElementById('edit-email').value = row.getAttribute('data-email');
            document.getElementById('edit-allowedLoginIps').value = row.getAttribute('data-allowedips');
          }
        }
        
        var editModal = new bootstrap.Modal(document.getElementById('editUserModal'));
        editModal.show();
      }
    }
  }
}