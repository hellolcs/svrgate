// 네트워크 객체 관련 자바스크립트

document.addEventListener('DOMContentLoaded', function() {
    // Select2 초기화
    initializeSelect2();
});

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
 * 네트워크 객체 수정 모달 열기
 */
function editNetworkObject(id) {
    // Ajax로 네트워크 객체 정보 가져오기
    $.ajax({
        url: '/object/network/' + id,
        type: 'GET',
        success: function(response) {
            // 폼에 데이터 채우기
            $('#edit-id').val(response.id);
            $('#edit-name').val(response.name);
            $('#edit-ipAddress').val(response.ipAddress);
            $('#edit-description').val(response.description);
            
            // Zone 설정
            $('#edit-zoneIds').val(response.zoneIds).trigger('change');
            
            // 모달 열기
            $('#editNetworkObjectModal').modal('show');
        },
        error: function(xhr, status, error) {
            alert('네트워크 객체 정보를 가져오는 중 오류가 발생했습니다: ' + error);
        }
    });
}

/**
 * 네트워크 객체 삭제
 */
function deleteNetworkObject(id) {
    if (confirm('이 네트워크 객체를 삭제하시겠습니까?')) {
        $('#deleteNetworkObjectId').val(id);
        $('#deleteNetworkObjectForm').submit();
    }
}