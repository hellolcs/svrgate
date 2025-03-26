// 정책관리 JS
$(document).ready(function() {
    // Select2 초기화
    $('.select2').select2({
        theme: 'bootstrap-5',
        width: '100%',
        placeholder: '선택하세요',
        allowClear: true
    });
    
    // 포트 모드 변경 이벤트 리스너 추가
    $('#portMode').on('change', function() {
        updatePortInputs('');
    });
    
    $('#edit-portMode').on('change', function() {
        updatePortInputs('edit-');
    });
    
    // 페이지 로드 시 초기화
    updatePortInputs('');
    updatePortInputs('edit-');
});

/**
 * 포트 입력 필드 업데이트
 * @param {string} prefix - '' 또는 'edit-' 접두사
 */
function updatePortInputs(prefix) {
    const portMode = $(`#${prefix}portMode`).val();
    const endPortField = $(`#${prefix}endPort`);
    
    if (portMode === 'multi') {
        // Multi 모드: 두 번째 입력 필드 활성화
        endPortField.prop('disabled', false).prop('required', true);
    } else {
        // Single 모드: 두 번째 입력 필드 비활성화하고 값 지우기
        endPortField.prop('disabled', true).prop('required', false).val('');
    }
}

// 나머지 함수들은 그대로 유지...
/**
 * 서버 패널 토글
 * @param {HTMLElement} header - 패널 헤더 요소
 */
function toggleServerPanel(header) {
    const panel = $(header).closest('.server-panel');
    panel.toggleClass('expanded');
    
    // 토글 아이콘 변경
    const icon = $(header).find('.toggle-icon');
    if (panel.hasClass('expanded')) {
        icon.removeClass('bi-chevron-right').addClass('bi-chevron-down');
        
        // 패널 확장 시 정책 로드
        const serverId = panel.data('server-id');
        loadPolicies(serverId);
    } else {
        icon.removeClass('bi-chevron-down').addClass('bi-chevron-right');
    }
}

/**
 * 정책 목록 로드
 * @param {number} serverId - 서버 ID
 */
function loadPolicies(serverId) {
    const tableBody = $(`#policy-table-${serverId} tbody`);
    
    // 이미 로드된 경우 중복 로드 방지
    if (tableBody.find('tr:first').find('td').length < 13) {
        $.ajax({
            url: `/rule/server/${serverId}`,
            type: 'GET',
            success: function(policies) {
                tableBody.empty();
                
                if (policies.length === 0) {
                    tableBody.append(`
                        <tr>
                            <td colspan="13" class="text-center">등록된 정책이 없습니다.</td>
                        </tr>
                    `);
                } else {
                    policies.forEach(policy => {
                        // 포트 표시 방식 변경
                        let portDisplay = policy.startPort;
                        if (policy.portMode === 'multi' && policy.endPort) {
                            portDisplay = `${policy.startPort} - ${policy.endPort}`;
                        }
                        
                        tableBody.append(`
                            <tr>
                                <td>${policy.priority}</td>
                                <td>${policy.sourceObjectName}</td>
                                <td>${policy.protocol.toUpperCase()}</td>
                                <td>${policy.portMode === 'single' ? 'Single' : 'Multi'}</td>
                                <td>${portDisplay}</td>
                                <td>${policy.action === 'accept' ? 'Accept' : 'Reject'}</td>
                                <td>${policy.timeLimit || '-'}</td>
                                <td>${policy.logging ? '사용' : '미사용'}</td>
                                <td>${policy.registrationDateFormatted}</td>
                                <td>${policy.requester}</td>
                                <td>${policy.registrar}</td>
                                <td>${policy.description || '-'}</td>
                                <td>
                                    <button type="button" class="btn btn-outline-primary btn-action" onclick="editPolicy(${policy.id})">수정</button>
                                    <button type="button" class="btn btn-outline-danger btn-action" onclick="deletePolicy(${policy.id})">삭제</button>
                                </td>
                            </tr>
                        `);
                    });
                }
            },
            error: function(xhr, status, error) {
                tableBody.html(`
                    <tr>
                        <td colspan="13" class="text-center text-danger">
                            정책 목록을 불러오는 중 오류가 발생했습니다: ${error}
                        </td>
                    </tr>
                `);
            }
        });
    }
}

/**
 * 정책 추가 모달 표시
 */
function showAddPolicyModal() {
    // 폼 초기화
    $('#addPolicyForm')[0].reset();
    $('#sourceObjectId').val('');
    $('#sourceObjectType').val('');
    $('#sourceObjectName').val('');
    
    // Select2 초기화
    $('#serverObjectId').val('').trigger('change');
    
    // 포트 입력 필드 초기화
    updatePortInputs('add');
    
    // 모달 표시
    $('#addPolicyModal').modal('show');
}

/**
 * 출발지 객체 검색 모달 열기
 * @param {string} targetForm - 'add' 또는 'edit'
 */
function openSourceSearchModal(targetForm) {
    // 전역 변수에 타겟 폼 저장
    window.currentSourceTarget = targetForm;
    
    // 검색 입력 필드 초기화
    $('#sourceSearchInput').val('');
    $('#sourceSearchResults').html('<tr><td colspan="5" class="text-center">검색어를 입력하고 검색 버튼을 클릭하세요.</td></tr>');
    
    // 모달 표시
    $('#sourceSearchModal').modal('show');
}

/**
 * 출발지 객체 검색
 */
function searchSourceObjects() {
    const searchText = $('#sourceSearchInput').val();
    const resultsContainer = $('#sourceSearchResults');
    
    // 로딩 표시
    resultsContainer.html('<tr><td colspan="5" class="text-center"><div class="spinner-border spinner-border-sm"></div> 검색 중...</td></tr>');
    
    $.ajax({
        url: '/rule/source-objects',
        type: 'GET',
        data: { searchText: searchText },
        success: function(objects) {
            resultsContainer.empty();
            
            if (objects.length === 0) {
                resultsContainer.html('<tr><td colspan="5" class="text-center">검색 결과가 없습니다.</td></tr>');
            } else {
                objects.forEach(obj => {
                    let typeLabel;
                    switch (obj.type) {
                        case 'SERVER':
                            typeLabel = '연동서버';
                            break;
                        case 'GENERAL':
                            typeLabel = '일반객체';
                            break;
                        case 'NETWORK':
                            typeLabel = '네트워크';
                            break;
                        default:
                            typeLabel = obj.type;
                    }
                    
                    resultsContainer.append(`
                        <tr>
                            <td>${typeLabel}</td>
                            <td>${obj.name}</td>
                            <td>${obj.ipAddress}</td>
                            <td>${obj.zone || '-'}</td>
                            <td>
                                <button type="button" class="btn btn-sm btn-primary" 
                                    onclick="selectSourceObject(${obj.id}, '${obj.type}', '${obj.name}')">
                                    선택
                                </button>
                            </td>
                        </tr>
                    `);
                });
            }
        },
        error: function(xhr, status, error) {
            resultsContainer.html(`<tr><td colspan="5" class="text-center text-danger">검색 중 오류가 발생했습니다: ${error}</td></tr>`);
        }
    });
}

/**
 * 출발지 객체 선택
 * @param {number} id - 객체 ID
 * @param {string} type - 객체 타입
 * @param {string} name - 객체 이름
 */
function selectSourceObject(id, type, name) {
    const prefix = window.currentSourceTarget === 'edit' ? 'edit-' : '';
    
    // 폼에 데이터 설정
    $(`#${prefix}sourceObjectId`).val(id);
    $(`#${prefix}sourceObjectType`).val(type);
    $(`#${prefix}sourceObjectName`).val(name);
    
    // 모달 닫기
    $('#sourceSearchModal').modal('hide');
}

/**
 * 정책 수정 모달 열기
 * @param {number} id - 정책 ID
 */
function editPolicy(id) {
    // 정책 정보 로드
    $.ajax({
        url: `/rule/${id}`,
        type: 'GET',
        success: function(policy) {
            // 폼에 데이터 설정
            $('#edit-id').val(policy.id);
            $('#edit-priority').val(policy.priority);
            $('#edit-sourceObjectId').val(policy.sourceObjectId);
            $('#edit-sourceObjectType').val(policy.sourceObjectType);
            $('#edit-sourceObjectName').val(policy.sourceObjectName);
            $('#edit-protocol').val(policy.protocol);
            $('#edit-portMode').val(policy.portMode);
            $('#edit-startPort').val(policy.startPort);
            $('#edit-endPort').val(policy.endPort);
            $('#edit-action').val(policy.action);
            $('#edit-timeLimit').val(policy.timeLimit);
            $('#edit-logging').val(policy.logging.toString());
            $('#edit-requester').val(policy.requester);
            $('#edit-description').val(policy.description);
            
            // 서버 ID 설정 (수정 불가)
            $('#edit-serverObjectId').val(policy.serverObjectId).trigger('change');
            $('#edit-serverObjectId-hidden').val(policy.serverObjectId);
            
            // 포트 입력 필드 업데이트
            updatePortInputs('edit');
            
            // 모달 표시
            $('#editPolicyModal').modal('show');
        },
        error: function(xhr, status, error) {
            alert(`정책 정보를 불러오는 중 오류가 발생했습니다: ${error}`);
        }
    });
}

/**
 * 정책 삭제
 * @param {number} id - 정책 ID
 */
function deletePolicy(id) {
    if (confirm('이 정책을 삭제하시겠습니까?')) {
        $('#deletePolicyId').val(id);
        $('#deletePolicyForm').submit();
    }
}

/**
 * 폼 검증 및 제출
 * @param {string} formId - 폼 ID
 */
function validateAndSubmit(formId) {
    const form = document.getElementById(formId);
    const prefix = formId === 'editPolicyForm' ? 'edit-' : '';
    
    // 포트 모드 확인
    const portMode = $(`#${prefix}portMode`).val();
    
    // Multi 모드인 경우 포트 범위 유효성 검사
    if (portMode === 'multi') {
        const startPort = parseInt($(`#${prefix}startPort`).val());
        const endPort = parseInt($(`#${prefix}endPort`).val());
        
        if (endPort <= startPort) {
            alert('종료 포트는 시작 포트보다 커야 합니다.');
            return false;
        }
    }
    
    // 모든 검증 통과 시 폼 제출
    form.submit();
}