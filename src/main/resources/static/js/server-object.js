// server-object.js - 연동서버 객체 관련 자바스크립트

// DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function() {
    initializeServerObjectPage();
});

/**
 * 연동서버 객체 페이지 초기화
 */
function initializeServerObjectPage() {
    // Select2 초기화
    initializeSelect2();
}

/**
 * Select2 초기화
 */
function initializeSelect2() {
    $('.select2').select2({
        theme: 'bootstrap-5',
        width: '100%',
        placeholder: '선택하세요',
        allowClear: true
    });
}

/**
 * 연동서버 객체 수정 모달 열기
 * @param {string} id 객체 ID
 */
function editServerObject(id) {
    // Ajax로 객체 정보 가져오기
    $.ajax({
        url: '/object/server/' + id,
        type: 'GET',
        success: function(response) {
            // 폼에 데이터 채우기
            $('#edit-id').val(response.id);
            $('#edit-name').val(response.name);
            $('#edit-ipAddress').val(response.ipAddress);
            $('#edit-active').prop('checked', response.active);
            $('#edit-description').val(response.description);
            $('#edit-apiKey').val(response.apiKey); // API Key 값 설정
            
            // Zone 선택
            $('#edit-zoneId').val(response.zoneId).trigger('change');
            
            // 모달 열기
            $('#editServerObjectModal').modal('show');
        },
        error: function(xhr, status, error) {
            alert('연동서버 객체 정보를 가져오는 중 오류가 발생했습니다: ' + error);
        }
    });
}

/**
 * 연동서버 객체 삭제
 * @param {string} id 객체 ID
 */
function deleteServerObject(id) {
    if (confirm('정말 이 연동서버 객체를 삭제하시겠습니까?')) {
        $('#deleteServerObjectId').val(id);
        $('#deleteServerObjectForm').submit();
    }
}