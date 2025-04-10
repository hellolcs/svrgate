// policy.js

// 전역 변수 선언
let currentPolicies = {}; // 서버별 정책 목록을 저장하는 객체
let globalSpinnerCount = 0; // 전역 스피너 활성화 카운터

// DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function() {
    initializePolicy();
    // 전역 스피너 DOM 요소 추가
    createGlobalSpinner();
});

/**
 * 정책 관리 페이지 초기화
 */
function initializePolicy() {
    // Select2 초기화
    initializeSelect2();
    
    // 엔터키로 소스 검색 실행
    $('#sourceSearchInput').on('keypress', function(e) {
        if (e.which === 13) { // 엔터키 코드
            searchSourceObjects();
            e.preventDefault();
        }
    });
    
    // 포트 모드 변경 이벤트 핸들러 설정
    $('#portMode').on('change', function() {
        updatePortInputs('');
    });
    
    $('#edit-portMode').on('change', function() {
        updatePortInputs('edit-');
    });
    
    // 폼 제출 이벤트 핸들러 설정
    configureFormSubmission();
    
    // 모달 이벤트 핸들러 설정
    setupModalEvents();
    
    // CSRF 토큰 설정을 위한 Ajax 기본 설정
    setupAjaxDefaults();
}

/**
 * Ajax 요청의 기본 설정
 */
function setupAjaxDefaults() {
    // CSRF 토큰 가져오기
    const csrfHeader = $("meta[name='_csrf_header']").attr("content");
    const csrfToken = $("meta[name='_csrf']").attr("content");
    
    // Ajax 기본 설정
    $.ajaxSetup({
        beforeSend: function(xhr) {
            if (csrfHeader && csrfToken) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        }
    });
}

/**
 * 전역 스피너 DOM 요소 생성
 */
function createGlobalSpinner() {
    // 스피너 컨테이너가 이미 있다면 생성하지 않음
    if ($('#global-spinner').length > 0) return;
    
    const spinnerHtml = `
    <div id="global-spinner" class="position-fixed w-100 h-100 top-0 start-0 d-none" style="z-index: 9999; background-color: rgba(0,0,0,0.4);">
        <div class="d-flex align-items-center justify-content-center h-100">
            <div class="bg-white p-4 rounded shadow-lg">
                <div class="text-center">
                    <div class="spinner-border text-primary" role="status" style="width: 3rem; height: 3rem;">
                        <span class="visually-hidden">처리 중...</span>
                    </div>
                    <h5 class="mt-3 mb-1">처리 중입니다</h5>
                    <p class="text-muted small mb-0">잠시만 기다려주세요.</p>
                    <p class="text-muted small mb-0" id="global-spinner-message">서버와 통신 중입니다.</p>
                </div>
            </div>
        </div>
    </div>
    `;
    
    // body에 스피너 요소 추가
    $('body').append(spinnerHtml);
}

/**
 * 전역 스피너 표시
 * @param {string} message 선택적 메시지
 */
function showGlobalSpinner(message) {
    globalSpinnerCount++;
    
    // 메시지 설정
    if (message) {
        $('#global-spinner-message').text(message);
    } else {
        $('#global-spinner-message').text('서버와 통신 중입니다.');
    }
    
    // 스피너 표시
    $('#global-spinner').removeClass('d-none').addClass('d-flex');
}

/**
 * 전역 스피너 숨김
 */
function hideGlobalSpinner() {
    globalSpinnerCount--;
    
    // 여러 API 요청이 동시에 진행될 수 있으므로 카운터가 0인 경우에만 스피너 숨김
    if (globalSpinnerCount <= 0) {
        globalSpinnerCount = 0;
        $('#global-spinner').removeClass('d-flex').addClass('d-none');
    }
}

/**
 * 모달 이벤트 핸들러 설정
 */
function setupModalEvents() {
    // 정책 추가 모달 초기화
    $('#addPolicyModal').on('show.bs.modal', function() {
        // 폼 초기화
        $('#addPolicyForm')[0].reset();
        
        // 출발지 객체 필드 초기화
        $('#sourceObjectId').val('');
        $('#sourceObjectType').val('');
        $('#sourceObjectName').val('');
        
        // Select2 초기화
        $('#serverObjectId').val('').trigger('change');
        
        // 포트 입력 필드 초기화
        updatePortInputs('');
        
        console.log('정책 추가 모달 초기화 완료');
    });
    
    // 출발지 검색 모달 이벤트
    $('#sourceSearchModal').on('shown.bs.modal', function() {
        console.log('출발지 검색 모달이 열렸습니다. Prefix:', $(this).data('prefix'));
        $('#sourceSearchInput').focus();
    });
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
 * 서버 패널 토글
 * @param {HTMLElement} headerElement 패널 헤더 요소
 */
function toggleServerPanel(header) {
    const panel = $(header).closest('.server-panel');
    
    // 이미 펼쳐져 있으면 접기
    if (panel.hasClass('expanded')) {
        panel.removeClass('expanded');
        panel.find('.toggle-icon').removeClass('bi-chevron-down').addClass('bi-chevron-right');
        return;
    }
    
    // 접혀있으면 펼치기
    panel.addClass('expanded');
    panel.find('.toggle-icon').removeClass('bi-chevron-right').addClass('bi-chevron-down');
    
    // 서버 ID 가져오기
    const serverId = panel.data('server-id');
    
    // 정책 테이블 요소
    const policyTable = $(`#policy-table-${serverId}`);
    const tbody = policyTable.find('tbody');
    
    // 항상 최신 데이터를 로드하도록 수정
    loadServerPolicies(serverId, tbody);
}

/**
 * 서버별 정책 목록 로드
 * @param {number} serverId 서버 ID
 * @param {jQuery} tbody 테이블 본문 요소
 */
function loadServerPolicies(serverId, tbody) {
    // 로딩 표시
    tbody.html('<tr><td colspan="14" class="text-center"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div><div>정책 로드 중...</div></td></tr>');
    
    // 서버별 정책 목록 가져오기
    $.ajax({
        url: `/rule/server/${serverId}`,
        type: 'GET',
        success: function(response) {
            currentPolicies[serverId] = response;
            renderPolicyTable(tbody, response);
        },
        error: function(xhr, status, error) {
            tbody.html(`<tr><td colspan="14" class="text-center text-danger">정책 로드 중 오류가 발생했습니다: ${error}</td></tr>`);
        }
    });
}

/**
 * 정책 테이블 렌더링
 */
function renderPolicyTable(tbody, policies) {
    if (!policies || policies.length === 0) {
        tbody.html('<tr><td colspan="14" class="text-center">등록된 정책이 없습니다.</td></tr>');
        return;
    }
    
    let html = '';
    policies.forEach(policy => {
        html += `
        <tr>
            <td>${policy.priority}</td>
            <td>${escapeHtml(policy.sourceObjectName)}</td>
            <td>${policy.protocol.toUpperCase()}</td>
            <td>${policy.portMode === 'single' ? '단일' : '범위'}</td>
            <td>${policy.portMode === 'single' ? policy.startPort : `${policy.startPort}-${policy.endPort}`}</td>
            <td>${policy.action === 'accept' ? '허용' : '거부'}</td>
            <td>${policy.timeLimit ? policy.timeLimit : '무제한'}</td>
            <td>${policy.expiresAtFormatted || '무기한'}</td>
            <td>${policy.logging ? '사용' : '미사용'}</td>
            <td>${policy.registrationDateFormatted}</td>
            <td>${policy.requester || '-'}</td>
            <td>${policy.registrar || '-'}</td>
            <td>${policy.description || '-'}</td>
            <td>
                <button type="button" class="btn btn-outline-primary btn-action" onclick="showEditPolicyModal(${policy.id})">수정</button>
                <button type="button" class="btn btn-outline-danger btn-action" onclick="confirmDeletePolicy(${policy.id})">삭제</button>
            </td>
        </tr>
        `;
    });
    
    tbody.html(html);
}

/**
 * 정책 추가 모달 표시
 */
function showAddPolicyModal() {
    // 모달 표시
    $('#addPolicyModal').modal('show');
}

/**
 * 정책 편집 모달 표시
 */
function showEditPolicyModal(id) {
    // 스피너 표시
    showGlobalSpinner('정책 정보를 불러오는 중입니다...');
    
    // 정책 상세 정보 가져오기
    $.ajax({
        url: `/rule/${id}`,
        type: 'GET',
        success: function(policy) {
            // 폼에 데이터 채우기
            $('#edit-id').val(policy.id);
            $('#edit-serverObjectId').val(policy.serverObjectId).trigger('change');
            $('#edit-serverObjectId-hidden').val(policy.serverObjectId);
            
            $('#edit-priority').val(policy.priority);
            $('#edit-priority-hidden').val(policy.priority);
            
            $('#edit-sourceObjectName').val(policy.sourceObjectName);
            $('#edit-sourceObjectId').val(policy.sourceObjectId);
            $('#edit-sourceObjectType').val(policy.sourceObjectType);
            
            $('#edit-protocol').val(policy.protocol);
            $('#edit-protocol-hidden').val(policy.protocol);
            
            $('#edit-portMode').val(policy.portMode);
            $('#edit-portMode-hidden').val(policy.portMode);
            
            $('#edit-startPort').val(policy.startPort);
            $('#edit-startPort-hidden').val(policy.startPort);
            
            $('#edit-endPort').val(policy.endPort);
            $('#edit-endPort-hidden').val(policy.endPort);
            
            $('#edit-action').val(policy.action);
            $('#edit-action-hidden').val(policy.action);
            
            $('#edit-timeLimit').val(policy.timeLimit);
            $('#edit-timeLimit-hidden').val(policy.timeLimit);
            
            $('#edit-logging').val(policy.logging.toString());
            $('#edit-logging-hidden').val(policy.logging);
            
            $('#edit-requester').val(policy.requester);
            $('#edit-description').val(policy.description);
            
            // 스피너 숨김
            hideGlobalSpinner();
            
            // 모달 표시
            $('#editPolicyModal').modal('show');
        },
        error: function(xhr, status, error) {
            // 스피너 숨김
            hideGlobalSpinner();
            alert('정책 정보를 가져오는 중 오류가 발생했습니다: ' + error);
        }
    });
}

/**
 * 출발지 검색 모달 열기
 */
function openSourceSearchModal(prefix) {
    // 현재 접두사 저장 (add 또는 edit)
    $('#sourceSearchModal').data('prefix', prefix);
    console.log('모달 열기 - 설정된 prefix:', prefix);
    
    // 검색 결과 초기화
    $('#sourceSearchResults').html('<tr><td colspan="5" class="text-center">검색어를 입력하고 검색 버튼을 클릭하세요.</td></tr>');
    
    // 검색어 초기화
    $('#sourceSearchInput').val('');
    
    // 모달 열기
    $('#sourceSearchModal').modal('show');
}

/**
 * 출발지 객체 검색
 */
function searchSourceObjects() {
    const searchText = $('#sourceSearchInput').val();
    const resultsContainer = $('#sourceSearchResults');
    
    // 로딩 표시
    resultsContainer.html('<tr><td colspan="5" class="text-center"><div class="spinner-border text-primary spinner-border-sm" role="status"><span class="visually-hidden">Loading...</span></div> 검색 중...</td></tr>');
    
    // 객체 검색 API 호출
    $.ajax({
        url: `/rule/source-objects?searchText=${encodeURIComponent(searchText)}`,
        type: 'GET',
        success: function(response) {
            if (!response || response.length === 0) {
                resultsContainer.html('<tr><td colspan="5" class="text-center">검색 결과가 없습니다.</td></tr>');
                return;
            }
            
            let html = '';
            response.forEach(obj => {
                // 특수문자 문제를 방지하기 위해 데이터 속성 사용
                html += `
                <tr>
                    <td>${getSourceTypeLabel(obj.type)}</td>
                    <td>${escapeHtml(obj.name)}</td>
                    <td>${escapeHtml(obj.ipAddress)}</td>
                    <td>${obj.zone ? escapeHtml(obj.zone) : '-'}</td>
                    <td>
                        <button type="button" class="btn btn-sm btn-primary source-select-btn" 
                                data-id="${obj.id}" 
                                data-type="${obj.type}" 
                                data-name="${escapeHtml(obj.name)}">
                            선택
                        </button>
                    </td>
                </tr>
                `;
            });
            
            resultsContainer.html(html);
            
            // 이벤트 위임 방식으로 변경
            resultsContainer.find('.source-select-btn').on('click', function() {
                const id = $(this).data('id');
                const type = $(this).data('type');
                const name = $(this).data('name');
                selectSourceObject(id, type, name);
            });
        },
        error: function(xhr, status, error) {
            resultsContainer.html(`<tr><td colspan="5" class="text-center text-danger">검색 중 오류가 발생했습니다: ${error}</td></tr>`);
        }
    });
}

/**
 * HTML 특수 문자 이스케이프
 */
function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * 출발지 객체 유형 라벨 반환
 */
function getSourceTypeLabel(type) {
    switch (type) {
        case 'SERVER': return '연동서버';
        case 'GENERAL': return '일반객체';
        case 'NETWORK': return '네트워크';
        default: return type;
    }
}

/**
 * 출발지 객체 선택
 */
function selectSourceObject(id, type, name) {
    try {
        // 디버깅용 로그 추가
        console.log('선택된 출발지 객체:', {id, type, name});
        
        const prefix = $('#sourceSearchModal').data('prefix') || '';
        console.log('현재 prefix:', prefix);
        
        // 선택된 출발지 객체 정보 설정
        $(`#${prefix}sourceObjectId`).val(id);
        $(`#${prefix}sourceObjectType`).val(type);
        $(`#${prefix}sourceObjectName`).val(name);
        
        // 값이 제대로 설정되었는지 확인
        console.log('설정된 값:', {
            id: $(`#${prefix}sourceObjectId`).val(),
            type: $(`#${prefix}sourceObjectType`).val(),
            name: $(`#${prefix}sourceObjectName`).val()
        });
        
        // 모달 닫기
        $('#sourceSearchModal').modal('hide');
    } catch (error) {
        console.error('출발지 객체 선택 중 오류 발생:', error);
        alert('출발지 객체 선택 중 오류가 발생했습니다. 개발자 도구의 콘솔을 확인하세요.');
    }
}

/**
 * 포트 입력 필드 업데이트
 */
function updatePortInputs(prefix) {
    const portMode = $(`#${prefix}portMode`).val();
    
    if (portMode === 'single') {
        // 단일 포트 모드: 종료 포트 비활성화
        $(`#${prefix}endPort`).prop('disabled', true).prop('required', false);
    } else if (portMode === 'multi') {
        // 범위 포트 모드: 종료 포트 활성화
        $(`#${prefix}endPort`).prop('disabled', false).prop('required', true);
    }
}

/**
 * 정책 삭제 확인
 */
function confirmDeletePolicy(id) {
    if (confirm('정말 이 정책을 삭제하시겠습니까?')) {
        deletePolicyWithApi(id);
    }
}

/**
 * API를 통한 정책 삭제
 */
function deletePolicyWithApi(id) {
    // 스피너 표시
    showGlobalSpinner('정책을 삭제하는 중입니다...');
    
    // API 호출 (POST 방식)
    $.ajax({
        url: '/rule/delete',
        type: 'POST',
        data: { id: id },
        success: function(response) {
            // 스피너 숨김
            hideGlobalSpinner();
            
            if (response.success) {
                // 성공 시 처리
                alert(response.message);
                
                // 현재 펼쳐진 패널 갱신 - 항상 서버에서 최신 데이터를 로드하도록 수정
                $('.server-panel.expanded').each(function() {
                    const serverId = $(this).data('server-id');
                    const tbody = $(`#policy-table-${serverId}`).find('tbody');
                    loadServerPolicies(serverId, tbody);
                });
            } else {
                // 실패 시 처리
                alert(response.message);
            }
        },
        error: function(xhr, status, error) {
            // 스피너 숨김
            hideGlobalSpinner();
            
            // 에러 처리
            let errorMessage = '정책 삭제 중 오류가 발생했습니다.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            }
            alert(errorMessage);
        }
    });
}

/**
 * 폼 제출 전 유효성 검사
 */
function validateAndSubmit(formId) {
    const form = document.getElementById(formId);
    
    console.log('폼 제출 검증 시작:', formId);
    
    // 출발지 객체 선택 여부 확인
    const prefix = formId === 'addPolicyForm' ? '' : 'edit-';
    const sourceObjectId = $(`#${prefix}sourceObjectId`).val();
    const sourceObjectType = $(`#${prefix}sourceObjectType`).val();
    
    console.log('출발지 객체 정보:', {
        id: sourceObjectId,
        type: sourceObjectType,
        name: $(`#${prefix}sourceObjectName`).val()
    });
    
    // 출발지 객체 선택 여부 검증
    if (!sourceObjectId || !sourceObjectType) {
        alert('출발지 객체를 선택해주세요.');
        return;
    }
    
    if (!form.checkValidity()) {
        // HTML5 기본 유효성 검사 수행
        form.reportValidity();
        return;
    }
    
    // 포트 범위 검증 (multi 모드에서 종료 포트가 시작 포트보다 큰지 확인)
    if ($(`#${prefix}portMode`).val() === 'multi') {
        const startPort = parseInt($(`#${prefix}startPort`).val());
        const endPort = parseInt($(`#${prefix}endPort`).val());
        
        if (endPort <= startPort) {
            alert('종료 포트는 시작 포트보다 커야 합니다.');
            return;
        }
    }
    
    // 폼 데이터 로깅 (디버깅용)
    const formData = new FormData(form);
    const formDataObj = {};
    formData.forEach((value, key) => {
        formDataObj[key] = value;
    });
    console.log('제출할 폼 데이터:', formDataObj);
    
    // 폼 ID에 따라 처리
    if (formId === 'addPolicyForm') {
        submitPolicyForm(form, true); // 추가 폼
    } else if (formId === 'editPolicyForm') {
        submitPolicyForm(form, false); // 수정 폼
    }
}

/**
 * 폼 제출 설정
 */
function configureFormSubmission() {
    // 폼이 HTML 기본 방식으로 제출되지 않도록 이벤트 리스너 설정
    $('#addPolicyForm, #editPolicyForm').on('submit', function(e) {
        e.preventDefault();
        return false;
    });
}

/**
 * 정책 폼 제출 (AJAX)
 * @param {HTMLFormElement} form 폼 요소
 * @param {boolean} isAdd 추가 여부 (true: 추가, false: 수정)
 */
function submitPolicyForm(form, isAdd) {
    // 폼 데이터 수집
    const formData = new FormData(form);
    
    // API URL 설정
    const url = isAdd ? '/rule/add' : '/rule/update';
    
    // 스피너 표시 (메시지 설정)
    showGlobalSpinner(isAdd ? '정책을 추가하는 중입니다...' : '정책을 수정하는 중입니다...');
    
    // API 호출
    $.ajax({
        url: url,
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(response) {
            // 스피너 숨김
            hideGlobalSpinner();
            
            if (response.success) {
                // 성공 시 처리
                alert(response.message);
                
                // 모달 닫기
                $(`#${isAdd ? 'add' : 'edit'}PolicyModal`).modal('hide');
                
                // 현재 펼쳐진 패널 갱신 - 항상 서버에서 최신 데이터를 로드하도록 수정
                $('.server-panel.expanded').each(function() {
                    const serverId = $(this).data('server-id');
                    const tbody = $(`#policy-table-${serverId}`).find('tbody');
                    loadServerPolicies(serverId, tbody);
                });
                
                // 폼 초기화 (추가인 경우)
                if (isAdd) {
                    form.reset();
                    $('#serverObjectId').val('').trigger('change');
                    $('#sourceObjectId').val('');
                    $('#sourceObjectType').val('');
                    $('#sourceObjectName').val('');
                }
            } else {
                // 실패 시 처리
                alert(response.message);
            }
        },
        error: function(xhr, status, error) {
            // 스피너 숨김
            hideGlobalSpinner();
            
            // 에러 처리
            let errorMessage = '정책 처리 중 오류가 발생했습니다.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            }
            alert(errorMessage);
        }
    });
}