// 정책관리 JS

// DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function() {
    initializePolicyPage();
});

/**
 * 정책 페이지 초기화
 */
function initializePolicyPage() {
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
    });
}
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
/**
 * 정책 목록 로드
 * @param {number} serverId - 서버 ID
 */
function loadPolicies(serverId) {
    const tableBody = $(`#policy-table-${serverId} tbody`);
    
    // 이미 로드된 경우 중복 로드 방지
    if (tableBody.find('tr:first').find('td').length < 14) { // 13에서 14로 변경
        $.ajax({
            url: `/rule/server/${serverId}`,
            type: 'GET',
            success: function(policies) {
                tableBody.empty();
                
                if (policies.length === 0) {
                    tableBody.append(`
                        <tr>
                            <td colspan="14" class="text-center">등록된 정책이 없습니다.</td>
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
                                <td>${policy.expiresAtFormatted || '무기한'}</td> <!-- 만료 시간 컬럼 추가 -->
                                <td>${policy.logging ? '사용' : '미사용'}</td>
                                <td>${policy.registrationDateFormatted}</td>
                                <td>${policy.requester || '-'}</td>
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
                        <td colspan="14" class="text-center text-danger"> <!-- 13에서 14로 변경 -->
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
    updatePortInputs('');
    
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
    // Ajax로 정책 정보 가져오기
    $.ajax({
        url: '/rule/' + id,
        type: 'GET',
        success: function(response) {
            // 폼에 데이터 채우기
            $('#edit-id').val(response.id);
            
            // 서버 객체 - select2는 trigger('change')가 필요함
            $('#edit-serverObjectId').val(response.serverObjectId).trigger('change');
            $('#edit-serverObjectId-hidden').val(response.serverObjectId);
            
            // 우선순위
            $('#edit-priority').val(response.priority);
            $('#edit-priority-hidden').val(response.priority);
            
            // 출발지 객체
            $('#edit-sourceObjectName').val(response.sourceObjectName);
            $('#edit-sourceObjectId').val(response.sourceObjectId);
            $('#edit-sourceObjectType').val(response.sourceObjectType);
            
            // 프로토콜
            $('#edit-protocol').val(response.protocol);
            $('#edit-protocol-hidden').val(response.protocol);
            
            // 포트 모드
            $('#edit-portMode').val(response.portMode);
            $('#edit-portMode-hidden').val(response.portMode);
            
            // 포트 범위
            $('#edit-startPort').val(response.startPort);
            $('#edit-startPort-hidden').val(response.startPort);
            $('#edit-endPort').val(response.endPort);
            $('#edit-endPort-hidden').val(response.endPort);
            
            // 동작
            $('#edit-action').val(response.action);
            $('#edit-action-hidden').val(response.action);
            
            // 시간제한
            $('#edit-timeLimit').val(response.timeLimit);
            $('#edit-timeLimit-hidden').val(response.timeLimit);
            
            // 로깅 - Boolean 값을 문자열로 변환하여 설정
            $('#edit-logging').val(String(response.logging)).trigger('change');
            $('#edit-logging-hidden').val(String(response.logging));
            
            // 수정 가능한 필드
            $('#edit-requester').val(response.requester);
            $('#edit-description').val(response.description);
            
            // select2 요소 재초기화
            setTimeout(function() {
                $('#edit-serverObjectId').select2({
                    theme: 'bootstrap-5',
                    width: '100%',
                    disabled: true
                });
            }, 100);
            
            // 모달 열기
            $('#editPolicyModal').modal('show');
        },
        error: function(xhr, status, error) {
            alert('정책 정보를 가져오는 중 오류가 발생했습니다: ' + error);
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
    
    // 필수 필드 검증
    const requiredFields = [
        { id: `${prefix}priority`, name: '우선순위' },
        { id: `${prefix}sourceObjectName`, name: '출발지' },
        { id: `${prefix}protocol`, name: '프로토콜' },
        { id: `${prefix}portMode`, name: '포트 모드' },
        { id: `${prefix}startPort`, name: '시작 포트' },
        { id: `${prefix}action`, name: '동작' },
        { id: `${prefix}logging`, name: '로깅' }
    ];
    
    for (let field of requiredFields) {
        const element = document.getElementById(field.id);
        if (!element.value) {
            alert(`${field.name} 값을 입력해주세요.`);
            element.focus();
            return false;
        }
    }
    
    // 포트 모드 확인
    const portMode = $(`#${prefix}portMode`).val();
    
    // Multi 모드인 경우 포트 범위 유효성 검사
    if (portMode === 'multi') {
        const startPort = parseInt($(`#${prefix}startPort`).val());
        const endPort = parseInt($(`#${prefix}endPort`).val());
        
        if (!$(`#${prefix}endPort`).val()) {
            alert('종료 포트를 입력해주세요.');
            $(`#${prefix}endPort`).focus();
            return false;
        }
        
        if (endPort <= startPort) {
            alert('종료 포트는 시작 포트보다 커야 합니다.');
            $(`#${prefix}endPort`).focus();
            return false;
        }
    }
    
    // 모든 검증 통과 시 폼 제출
    form.submit();
}