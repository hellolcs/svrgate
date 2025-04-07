// policy.js - 정책관리 페이지 스크립트

// CSRF 토큰 설정
const token = $("meta[name='_csrf']").attr("content");
const header = $("meta[name='_csrf_header']").attr("content");

// AJAX 요청 설정에 CSRF 토큰 추가
$.ajaxSetup({
    beforeSend: function(xhr) {
        if (token && header) {
            xhr.setRequestHeader(header, token);
        }
    }
});

// 현재 열려있는 모달
let currentModal = null;
// 출발지 선택 대상 필드 (추가 또는 수정 구분용)
let sourceSelectionTarget = '';

// DOM이 로드된 후 실행
$(document).ready(function() {
    // Select2 초기화
    initializeSelect2();
    
    // 검색 조건이 있는 경우에 대한 처리
    if (typeof initialSearchText !== 'undefined' && initialSearchText ||
        typeof initialSearchType !== 'undefined' && initialSearchType ||
        typeof initialServerId !== 'undefined' && initialServerId) {
        // AJAX로 검색 결과를 가져와서 표시
        fetchSearchResults();
    }
});

/**
 * Select2 플러그인 초기화
 */
function initializeSelect2() {
    $('.select2, .form-select').select2({
        theme: 'bootstrap-5',
        width: '100%'
    });
}

/**
 * 서버 패널 토글
 */
function toggleServerPanel(header) {
    const panel = $(header).closest('.server-panel');
    const tableContainer = panel.find('.policy-table-container');
    const toggleIcon = panel.find('.toggle-icon');
    
    if (panel.hasClass('expanded')) {
        // 패널 닫기
        panel.removeClass('expanded');
        tableContainer.slideUp(300);
        toggleIcon.removeClass('bi-chevron-down').addClass('bi-chevron-right');
    } else {
        // 패널 열기
        panel.addClass('expanded');
        tableContainer.slideDown(300);
        toggleIcon.removeClass('bi-chevron-right').addClass('bi-chevron-down');
        
        // 서버 ID 가져오기
        const serverId = panel.data('server-id');
        
        // 정책 목록 로드
        loadPolicies(serverId);
    }
}

/**
 * 서버별 정책 목록 로드
 */
function loadPolicies(serverId) {
    const tableId = 'policy-table-' + serverId;
    const table = document.getElementById(tableId);
    
    if (!table) {
        console.error('Table element not found:', tableId);
        return;
    }
    
    const tbody = table.querySelector('tbody');
    
    // 로딩 표시
    tbody.innerHTML = `
        <tr>
            <td colspan="14" class="text-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <div>정책 목록을 불러오는 중입니다...</div>
            </td>
        </tr>
    `;
    
    // 서버별 정책 목록 AJAX 요청
    $.ajax({
        url: '/rule/server/' + serverId,
        type: 'GET',
        success: function(policies) {
            // 테이블 내용 초기화
            tbody.innerHTML = '';
            
            if (policies.length === 0) {
                // 정책이 없는 경우
                const emptyRow = document.createElement('tr');
                emptyRow.innerHTML = '<td colspan="14" class="text-center">등록된 정책이 없습니다.</td>';
                tbody.appendChild(emptyRow);
            } else {
                // 정책 목록 표시
                policies.forEach(policy => {
                    const row = document.createElement('tr');
                    row.setAttribute('data-id', policy.id);
                    
                    // 셀 생성
                    row.innerHTML = `
                        <td>${policy.priority}</td>
                        <td>${policy.sourceObjectName || ''}</td>
                        <td>${policy.protocol || ''}</td>
                        <td>${policy.portMode || ''}</td>
                        <td>${getPortDisplay(policy)}</td>
                        <td>${policy.action || ''}</td>
                        <td>${policy.timeLimit || ''}</td>
                        <td>${policy.expiresAtFormatted || ''}</td>
                        <td>${policy.logging ? '사용' : '미사용'}</td>
                        <td>${policy.registrationDateFormatted || ''}</td>
                        <td>${policy.requester || ''}</td>
                        <td>${policy.registrar || ''}</td>
                        <td>${policy.description || ''}</td>
                        <td>
                            <button type="button" class="btn btn-outline-primary btn-sm" onclick="editPolicy(${policy.id})">수정</button>
                            <button type="button" class="btn btn-outline-danger btn-sm" onclick="deletePolicy(${policy.id})">삭제</button>
                        </td>
                    `;
                    
                    tbody.appendChild(row);
                });
            }
        },
        error: function(xhr, status, error) {
            console.error('정책 목록 로드 실패:', error);
            tbody.innerHTML = `
                <tr>
                    <td colspan="14" class="text-center text-danger">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        정책 목록을 불러오는 중 오류가 발생했습니다.
                    </td>
                </tr>
            `;
        }
    });
}

/**
 * 정책 추가 모달 표시
 */
function showAddPolicyModal() {
    // 모달 초기화
    $('#addPolicyForm')[0].reset();
    
    // 출발지 필드 초기화
    $('#sourceObjectName').val('');
    $('#sourceObjectId').val('');
    $('#sourceObjectType').val('');
    
    // 포트 모드 초기화
    updatePortInputs('');
    
    // 모달 표시
    $('#addPolicyModal').modal('show');
}

/**
 * 정책 편집 모달 표시
 */
function editPolicy(id) {
    // 정책 정보 AJAX 요청
    $.ajax({
        url: '/rule//' + id,
        type: 'GET',
        success: function(policy) {
            // 폼 필드 설정
            $('#edit-id').val(policy.id);
            
            // 서버 ID (수정 불가)
            $('#edit-serverObjectId').val(policy.serverObjectId).trigger('change');
            $('#edit-serverObjectId-hidden').val(policy.serverObjectId);
            
            // 우선순위 (수정 불가)
            $('#edit-priority').val(policy.priority);
            $('#edit-priority-hidden').val(policy.priority);
            
            // 출발지 (수정 불가)
            $('#edit-sourceObjectName').val(policy.sourceObjectName);
            $('#edit-sourceObjectId').val(policy.sourceObjectId);
            $('#edit-sourceObjectType').val(policy.sourceObjectType);
            
            // 프로토콜 (수정 불가)
            $('#edit-protocol').val(policy.protocol);
            $('#edit-protocol-hidden').val(policy.protocol);
            
            // 포트 모드 (수정 불가)
            $('#edit-portMode').val(policy.portMode);
            $('#edit-portMode-hidden').val(policy.portMode);
            
            // 포트 범위 (수정 불가)
            $('#edit-startPort').val(policy.startPort);
            $('#edit-startPort-hidden').val(policy.startPort);
            
            if (policy.portMode === 'multi' && policy.endPort) {
                $('#edit-endPort').val(policy.endPort);
            } else {
                $('#edit-endPort').val(policy.startPort);
            }
            $('#edit-endPort-hidden').val(policy.endPort || policy.startPort);
            
            // 동작 (수정 불가)
            $('#edit-action').val(policy.action);
            $('#edit-action-hidden').val(policy.action);
            
            // 시간 제한 (수정 불가)
            $('#edit-timeLimit').val(policy.timeLimit);
            $('#edit-timeLimit-hidden').val(policy.timeLimit);
            
            // 로깅 (수정 불가)
            $('#edit-logging').val(policy.logging.toString());
            $('#edit-logging-hidden').val(policy.logging.toString());
            
            // 요청자 (수정 가능)
            $('#edit-requester').val(policy.requester);
            
            // 설명 (수정 가능)
            $('#edit-description').val(policy.description);
            
            // 모달 표시
            $('#editPolicyModal').modal('show');
        },
        error: function(xhr, status, error) {
            console.error('정책 정보 로드 실패:', error);
            alert('정책 정보를 불러오는 중 오류가 발생했습니다.');
        }
    });
}

/**
 * 정책 삭제 확인
 */
function deletePolicy(id) {
    if (confirm('이 정책을 삭제하시겠습니까?')) {
        // 삭제 요청 (AJAX)
        $.ajax({
            url: '/rule/delete',
            type: 'POST',
            data: { id: id },
            success: function(response) {
                if (response.success) {
                    // 성공 메시지
                    alert(response.message);
                    
                    // 페이지 새로고침
                    location.reload();
                } else {
                    // 실패 메시지
                    alert(response.message);
                }
            },
            error: function(xhr, status, error) {
                console.error('정책 삭제 실패:', error);
                alert('정책 삭제 중 오류가 발생했습니다.');
            }
        });
    }
}

/**
 * 출발지 검색 모달 열기
 */
function openSourceSearchModal(target) {
    // 대상 저장 (추가 또는 수정 구분용)
    sourceSelectionTarget = target;
    
    // 검색 결과 초기화
    $('#sourceSearchResults').html('<tr><td colspan="5" class="text-center">검색어를 입력하고 검색 버튼을 클릭하세요.</td></tr>');
    
    // 검색어 초기화
    $('#sourceSearchInput').val('');
    
    // 모달 표시
    $('#sourceSearchModal').modal('show');
}

/**
 * 출발지 객체 검색
 */
function searchSourceObjects() {
    // 검색어 가져오기
    const searchText = $('#sourceSearchInput').val();
    
    // 검색 결과 영역
    const resultsContainer = $('#sourceSearchResults');
    
    // 로딩 표시
    resultsContainer.html(`
        <tr>
            <td colspan="5" class="text-center">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <div>출발지 객체 검색 중...</div>
            </td>
        </tr>
    `);
    
    // AJAX 요청
    $.ajax({
        url: '/rule/source-objects',
        type: 'GET',
        data: { searchText: searchText },
        success: function(objects) {
            if (objects.length === 0) {
                // 검색 결과가 없는 경우
                resultsContainer.html('<tr><td colspan="5" class="text-center">검색 결과가 없습니다.</td></tr>');
            } else {
                // 검색 결과 표시
                resultsContainer.empty();
                
                objects.forEach(obj => {
                    const row = document.createElement('tr');
                    
                    // 객체 유형 매핑
                    let typeDisplay = '';
                    switch (obj.type) {
                        case 'SERVER':
                            typeDisplay = '연동서버';
                            break;
                        case 'GENERAL':
                            typeDisplay = '일반';
                            break;
                        case 'NETWORK':
                            typeDisplay = '네트워크';
                            break;
                        default:
                            typeDisplay = obj.type;
                    }
                    
                    // 셀 생성
                    row.innerHTML = `
                        <td>${typeDisplay}</td>
                        <td>${obj.name || ''}</td>
                        <td>${obj.ipAddress || ''}</td>
                        <td>${obj.zone || ''}</td>
                        <td>
                            <button type="button" class="btn btn-sm btn-primary" 
                                    onclick="selectSourceObject('${obj.id}', '${obj.type}', '${obj.name}')">
                                선택
                            </button>
                        </td>
                    `;
                    
                    resultsContainer.append(row);
                });
            }
        },
        error: function(xhr, status, error) {
            console.error('출발지 객체 검색 실패:', error);
            resultsContainer.html(`
                <tr>
                    <td colspan="5" class="text-center text-danger">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        출발지 객체 검색 중 오류가 발생했습니다.
                    </td>
                </tr>
            `);
        }
    });
}

/**
 * 출발지 객체 선택
 */
function selectSourceObject(id, type, name) {
    // prefix가 있으면 (edit-) 수정 모달에 값 설정, 없으면 추가 모달에 값 설정
    const prefix = sourceSelectionTarget;
    
    // 필드 ID 구성
    const sourceObjectNameId = prefix + 'sourceObjectName';
    const sourceObjectIdId = prefix + 'sourceObjectId';
    const sourceObjectTypeId = prefix + 'sourceObjectType';
    
    // 필드 값 설정
    document.getElementById(sourceObjectNameId).value = name;
    document.getElementById(sourceObjectIdId).value = id;
    document.getElementById(sourceObjectTypeId).value = type;
    
    // 모달 닫기
    $('#sourceSearchModal').modal('hide');
}

/**
 * 포트 모드에 따라 포트 입력 필드 업데이트
 */
function updatePortInputs(prefix) {
    // 포트 모드 가져오기
    const portMode = document.getElementById(prefix + 'portMode').value;
    
    // 종료 포트 필드
    const endPortField = document.getElementById(prefix + 'endPort');
    
    if (portMode === 'single') {
        // 단일 포트 모드일 경우 종료 포트 비활성화
        endPortField.disabled = true;
        endPortField.required = false;
        endPortField.value = '';
    } else if (portMode === 'multi') {
        // 다중 포트 모드일 경우 종료 포트 활성화
        endPortField.disabled = false;
        endPortField.required = true;
    }
}

/**
 * 폼 유효성 검사 및 제출
 */
function validateAndSubmit(formId) {
    const form = document.getElementById(formId);
    
    if (form.checkValidity()) {
        // AJAX 폼 제출
        const formData = new FormData(form);
        
        $.ajax({
            url: form.action,
            type: 'POST',
            data: new URLSearchParams(formData).toString(),
            contentType: 'application/x-www-form-urlencoded',
            success: function(response) {
                if (response.success) {
                    // 성공 메시지
                    alert(response.message);
                    
                    // 모달 닫기
                    $('.modal').modal('hide');
                    
                    // 페이지 새로고침
                    location.reload();
                } else {
                    // 실패 메시지
                    alert(response.message);
                }
            },
            error: function(xhr, status, error) {
                console.error('폼 제출 실패:', error);
                alert('폼 제출 중 오류가 발생했습니다.');
            }
        });
    } else {
        // 유효성 검사 실패 시 HTML5 기본 유효성 검사 메시지 표시
        form.reportValidity();
    }
}

/**
 * 포트 표시 형식 생성
 */
function getPortDisplay(policy) {
    if (policy.portMode === 'single') {
        return policy.startPort;
    } else if (policy.portMode === 'multi') {
        return `${policy.startPort} - ${policy.endPort}`;
    }
    return '';
}

/**
 * 검색 결과 가져오기
 */
function fetchSearchResults() {
    // 검색 중임을 표시 (로딩 스피너 등)
    showLoading(true);
    
    // 검색 조건 가져오기
    const serverId = $('#filterServer').val() || initialServerId;
    const searchType = $('#searchType').val() || initialSearchType;
    const searchText = $('input[name="searchText"]').val() || initialSearchText;
    const size = $('select[name="size"]').val() || initialSize || 30;
    
    // AJAX 요청
    $.ajax({
        url: '/rule/search',
        type: 'GET',
        data: {
            serverId: serverId,
            searchType: searchType,
            searchText: searchText,
            size: size
        },
        success: function(response) {
            // 서버 패널 접기
            $('.server-panel').removeClass('expanded');
            
            // 검색 결과가 있는 서버 패널만 펼치기
            if (response && response.content && response.content.length > 0) {
                // 서버 ID로 그룹화
                const groupedPolicies = groupPoliciesByServer(response.content);
                
                // 각 서버 패널에 검색 결과 표시
                for (const [serverId, policies] of Object.entries(groupedPolicies)) {
                    const panel = $(`.server-panel[data-server-id="${serverId}"]`);
                    if (panel.length) {
                        // 패널 펼치기
                        panel.addClass('expanded');
                        const tableContainer = panel.find('.policy-table-container');
                        tableContainer.show();
                        
                        // 아이콘 변경
                        const toggleIcon = panel.find('.toggle-icon');
                        toggleIcon.removeClass('bi-chevron-right');
                        toggleIcon.addClass('bi-chevron-down');
                        
                        // 정책 목록 표시
                        displayPolicies(serverId, policies);
                    }
                }
            } else {
                // 검색 결과가 없는 경우 메시지 표시
                alert('검색 결과가 없습니다.');
            }
            
            showLoading(false);
        },
        error: function(xhr, status, error) {
            console.error('Error fetching search results:', error);
            showLoading(false);
            // 에러 메시지 표시
            alert('검색 결과를 가져오는 중 오류가 발생했습니다.');
        }
    });
}

/**
 * 정책을 서버 ID로 그룹화
 */
function groupPoliciesByServer(policies) {
    const groupedPolicies = {};
    
    policies.forEach(policy => {
        const serverId = policy.serverObjectId;
        if (!groupedPolicies[serverId]) {
            groupedPolicies[serverId] = [];
        }
        groupedPolicies[serverId].push(policy);
    });
    
    return groupedPolicies;
}

/**
 * 특정 서버의 정책 목록 표시
 */
function displayPolicies(serverId, policies) {
    const tableId = 'policy-table-' + serverId;
    const table = document.getElementById(tableId);
    
    if (!table) {
        console.error('Table element not found:', tableId);
        return;
    }
    
    const tbody = table.querySelector('tbody');
    
    // 테이블 내용 초기화
    tbody.innerHTML = '';
    
    if (policies.length === 0) {
        // 정책이 없는 경우
        const emptyRow = document.createElement('tr');
        emptyRow.innerHTML = '<td colspan="14" class="text-center">등록된 정책이 없습니다.</td>';
        tbody.appendChild(emptyRow);
    } else {
        // 정책 목록 표시
        policies.forEach(policy => {
            const row = document.createElement('tr');
            row.setAttribute('data-id', policy.id);
            
            // 셀 생성
            row.innerHTML = `
                <td>${policy.priority}</td>
                <td>${policy.sourceObjectName || ''}</td>
                <td>${policy.protocol || ''}</td>
                <td>${policy.portMode || ''}</td>
                <td>${getPortDisplay(policy)}</td>
                <td>${policy.action || ''}</td>
                <td>${policy.timeLimit || ''}</td>
                <td>${policy.expiresAtFormatted || ''}</td>
                <td>${policy.logging ? '사용' : '미사용'}</td>
                <td>${policy.registrationDateFormatted || ''}</td>
                <td>${policy.requester || ''}</td>
                <td>${policy.registrar || ''}</td>
                <td>${policy.description || ''}</td>
                <td>
                    <button type="button" class="btn btn-outline-primary btn-sm" onclick="editPolicy(${policy.id})">수정</button>
                    <button type="button" class="btn btn-outline-danger btn-sm" onclick="deletePolicy(${policy.id})">삭제</button>
                </td>
            `;
            
            tbody.appendChild(row);
        });
    }
}

/**
 * 로딩 스피너 표시/숨기기
 */
function showLoading(show) {
    if (show) {
        // 글로벌 스피너가 없으면 생성
        if (!$('#global-spinner').length) {
            const spinner = $(`
                <div id="global-spinner">
                    <div class="spinner-container">
                        <div class="spinner-border text-primary" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                        <div class="mt-2">검색 중...</div>
                    </div>
                </div>
            `);
            $('body').append(spinner);
        } else {
            $('#global-spinner').show();
        }
    } else {
        $('#global-spinner').hide();
    }
}