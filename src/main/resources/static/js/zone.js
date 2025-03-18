// Zone 페이지 관련 자바스크립트
// 재귀 호출 방지를 위한 플래그
let isUpdating = false;
let isEditUpdating = false;

// DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function() {
    initializeZonePage();
});

/**
 * Zone 페이지 초기화
 */
function initializeZonePage() {
    // Select2 초기화
    initializeSelect2();
    // 이벤트 핸들러 설정
    setupEventHandlers();
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
 * 이벤트 핸들러 설정
 */
function setupEventHandlers() {
    // 비보안Zone 선택 시 이벤트 (추가 모달)
    $('#nonSecureZoneIds').on('change', function() {
        handleZoneSelection($(this), $('#secureZoneIds'), isUpdating);
    });
    
    // 보안Zone 선택 시 이벤트 (추가 모달)
    $('#secureZoneIds').on('change', function() {
        handleZoneSelection($(this), $('#nonSecureZoneIds'), isUpdating);
    });
    
    // 비보안Zone 선택 시 이벤트 (수정 모달)
    $('#edit-nonSecureZoneIds').on('change', function() {
        handleZoneSelection($(this), $('#edit-secureZoneIds'), isEditUpdating, $('#edit-id').val());
    });
    
    // 보안Zone 선택 시 이벤트 (수정 모달)
    $('#edit-secureZoneIds').on('change', function() {
        handleZoneSelection($(this), $('#edit-nonSecureZoneIds'), isEditUpdating, $('#edit-id').val());
    });
}

/**
 * Zone 선택 처리 공통 함수
 * @param {jQuery} sourceSelect 이벤트가 발생한 select 요소
 * @param {jQuery} targetSelect 반대편 select 요소
 * @param {boolean} updatingFlag 재귀 호출 방지 플래그 참조
 * @param {string} selfId 자기 자신 ID (수정 모달에서만 사용)
 */
function handleZoneSelection(sourceSelect, targetSelect, updatingFlag, selfId) {
    if (window[updatingFlag]) return; // 재귀 호출 방지
    
    window[updatingFlag] = true;
    try {
        let sourceSelected = sourceSelect.val() || [];
        
        // 선택된 항목들을 타겟 select에서 비활성화
        targetSelect.find('option').each(function() {
            if (sourceSelected.includes($(this).val())) {
                $(this).prop('disabled', true);
            } else if (selfId && $(this).val() === selfId) {
                // 수정 모달에서 자기 자신은 항상 비활성화
                $(this).prop('disabled', true);
            } else {
                $(this).prop('disabled', false);
            }
        });
        
        // 현재 타겟 선택 항목 중에서 소스에서 선택된 항목 제거
        let targetSelected = targetSelect.val() || [];
        targetSelected = targetSelected.filter(id => !sourceSelected.includes(id));
        
        // 값만 변경하고 UI 업데이트
        targetSelect.val(targetSelected);
        targetSelect.select2({
            theme: 'bootstrap-5',
            width: '100%',
            placeholder: '선택하세요',
            allowClear: true
        });
    } finally {
        window[updatingFlag] = false;
    }
}

/**
 * Zone 수정 모달 열기
 * @param {string} id Zone ID
 */
function editZone(id) {
    // Ajax로 Zone 정보 가져오기
    $.ajax({
        url: '/object/zone/' + id,
        type: 'GET',
        success: function(response) {
            // 폼에 데이터 채우기
            $('#edit-id').val(response.id);
            $('#edit-name').val(response.name);
            $('#edit-firewallIp').val(response.firewallIp);
            $('#edit-active').prop('checked', response.active);
            $('#edit-description').val(response.description);
            
            // 자기 자신을 제외하기 위해 disable 처리
            $('#edit-nonSecureZoneIds option[value="' + response.id + '"]').prop('disabled', true);
            $('#edit-secureZoneIds option[value="' + response.id + '"]').prop('disabled', true);
            
            // Select2 다시 초기화
            $('#edit-nonSecureZoneIds').select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: '선택하세요',
                allowClear: true
            });
            
            $('#edit-secureZoneIds').select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: '선택하세요',
                allowClear: true
            });
            
            // 타이밍 이슈를 해결하기 위해 setTimeout 사용
            setTimeout(() => {
                // 비보안Zone 및 보안Zone 값 설정
                if (response.nonSecureZoneIds && response.nonSecureZoneIds.length > 0) {
                    $('#edit-nonSecureZoneIds').val(response.nonSecureZoneIds).trigger('change');
                } else {
                    $('#edit-nonSecureZoneIds').val([]).trigger('change');
                }
                
                if (response.secureZoneIds && response.secureZoneIds.length > 0) {
                    $('#edit-secureZoneIds').val(response.secureZoneIds).trigger('change');
                } else {
                    $('#edit-secureZoneIds').val([]).trigger('change');
                }
                
                // 모달 열기
                $('#editZoneModal').modal('show');
            }, 100);
        },
        error: function(xhr, status, error) {
            alert('Zone 정보를 가져오는 중 오류가 발생했습니다: ' + error);
        }
    });
}

/**
 * Zone 삭제
 * @param {string} id Zone ID
 */
function deleteZone(id) {
    if (confirm('정말 이 Zone을 삭제하시겠습니까?')) {
        $('#deleteZoneId').val(id);
        $('#deleteZoneForm').submit();
    }
}