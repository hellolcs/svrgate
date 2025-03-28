// policy.js - 정책 관리 관련 자바스크립트

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    initializePolicyPage();
});

/**
 * 정책 페이지 초기화
 */
function initializePolicyPage() {
    // Select2 초기화
    initializeSelect2();
    
    // 포트 모드 변경 이벤트 리스너 추가
    $('#portMode').on('change', function() {
        updatePortInputs('');
    });
    
    $('#edit-portMode').on('change', function() {
        updatePortInputs('edit-');
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
 * @param {HTMLElement} headerElement 클릭된 카드 헤더 요소
 */
function toggleServerPanel(headerElement) {
    const panel = headerElement.closest('.server-panel');
    const isExpanded = panel.classList.contains('expanded');
    const toggleIcon = headerElement.querySelector('.toggle-icon');
    const tableContainer = panel.querySelector('.policy-table-container');
    const serverId = panel.getAttribute('data-server-id');
    
    if (isExpanded) {
        // 패널 접기
        panel.classList.remove('expanded');
        toggleIcon.classList.remove('bi-chevron-down');
        toggleIcon.classList.add('bi-chevron-right');
        tableContainer.style.display = 'none';
    } else {
        // 패널 펼치기
        panel.classList.add('expanded');
        toggleIcon.classList.remove('bi-chevron-right');
        toggleIcon.classList.add('bi-chevron-down');
        tableContainer.style.display = 'block';
        
        // 정책 데이터 로드 (Ajax)
        loadPoliciesForServer(serverId);
    }
}

/**
 * 서버별 정책 데이터 로드
 * @param {string} serverId 서버 ID
 */
function loadPoliciesForServer(serverId) {
    const tableBody = document.querySelector(`#policy-table-${serverId} tbody`);
    
    // 이미 로드한 데이터가 있는지 확인
    const firstRow = tableBody.querySelector('tr');
    if (firstRow && firstRow.cells.length === 1 && firstRow.cells[0].colSpan) {
        // 로딩 중 표시
        tableBody.innerHTML = `
            <tr>
                <td colspan="14" class="text-center">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </td>
            </tr>
        `;
        
        // Ajax 요청
        $.ajax({
            url: `/rule/server/${serverId}`,
            type: 'GET',
            success: function(policies) {
                renderPolicyTable(tableBody, policies);
            },
            error: function(xhr, status, error) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="14" class="text-center text-danger">
                            <i class="bi bi-exclamation-triangle me-2"></i>정책 로드 중 오류가 발생했습니다: ${error}
                        </td>
                    </tr>
                `;
            }
        });
    }
}

/**
 * 정책 테이블 렌더링
 * @param {HTMLElement} tableBody 테이블 바디 요소
 * @param {Array} policies 정책 데이터 배열
 */
function renderPolicyTable(tableBody, policies) {
    if (!policies || policies.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="14" class="text-center">
                    등록된 정책이 없습니다.
                </td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    policies.forEach(policy => {
        // 포트 표시 형식 처리
        let portDisplay = policy.portMode === 'single' 
            ? policy.startPort 
            : `${policy.startPort} - ${policy.endPort}`;
        
        // 로깅 표시 형식 처리
        let loggingDisplay = policy.logging ? '사용' : '미사용';
        
        html += `
            <tr>
                <td>${policy.priority}</td>
                <td>${policy.sourceObjectName || ''}</td>
                <td>${policy.protocol || ''}</td>
                <td>${policy.portMode === 'single' ? 'Single' : 'Multi'}</td>
                <td>${portDisplay}</td>
                <td>${policy.action === 'accept' ? 'Accept' : 'Reject'}</td>
                <td>${policy.timeLimit || '-'}</td>
                <td>${policy.expiresAtFormatted || '무기한'}</td>
                <td>${loggingDisplay}</td>
                <td>${policy.registrationDateFormatted || ''}</td>
                <td>${policy.requester || ''}</td>
                <td>${policy.registrar || ''}</td>
                <td>${policy.description || ''}</td>
                <td>
                    <button type="button" class="btn btn-outline-primary btn-action" 
                            onclick="editPolicy(${policy.id})">수정</button>
                    <button type="button" class="btn btn-outline-danger btn-action" 
                            onclick="deletePolicy(${policy.id})">삭제</button>
                </td>
            </tr>
        `;
    });
    
    tableBody.innerHTML = html;
}

/**
 * 정책 추가 모달 표시
 */
function showAddPolicyModal() {
    // 폼 초기화
    document.getElementById('addPolicyForm').reset();
    document.getElementById('sourceObjectName').value = '';
    document.getElementById('sourceObjectId').value = '';
    document.getElementById('sourceObjectType').value = '';
    document.getElementById('endPort').disabled = true;
    
    // Select2 초기화
    $('#serverObjectId').val('').trigger('change');
    
    // 모달 표시
    const modal = new bootstrap.Modal(document.getElementById('addPolicyModal'));
    modal.show();
}

/**
 * 포트 모드에 따라 포트 입력 필드 활성화/비활성화
 * @param {string} prefix 접두사 ('edit-' 또는 '')
 */
function updatePortInputs(prefix) {
    const portMode = document.getElementById(prefix + 'portMode').value;
    const endPortInput = document.getElementById(prefix + 'endPort');
    
    if (portMode === 'multi') {
        endPortInput.disabled = false;
        endPortInput.required = true;
    } else {
        endPortInput.disabled = true;
        endPortInput.required = false;
        endPortInput.value = '';
    }
}

/**
 * 출발지 검색 모달 열기
 * @param {string} mode 모드 ('add' 또는 'edit')
 */
function openSourceSearchModal(mode) {
    // 검색 입력 초기화
    document.getElementById('sourceSearchInput').value = '';
    document.getElementById('sourceSearchResults').innerHTML = `
        <tr>
            <td colspan="5" class="text-center">검색어를 입력하고 검색 버튼을 클릭하세요.</td>
        </tr>
    `;
    
    // 현재 모드 저장 (전역 변수)
    window.currentSourceSearchMode = mode;
    
    // 모달 표시
    const modal = new bootstrap.Modal(document.getElementById('sourceSearchModal'));
    modal.show();
}

/**
 * 출발지 객체 검색
 */
function searchSourceObjects() {
    const searchText = document.getElementById('sourceSearchInput').value;
    const resultsContainer = document.getElementById('sourceSearchResults');
    
    // 로딩 표시
    resultsContainer.innerHTML = `
        <tr>
            <td colspan="5" class="text-center">
                <div class="spinner-border spinner-border-sm text-primary" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <span class="ms-2">검색 중...</span>
            </td>
        </tr>
    `;
    
    // Ajax 요청
    $.ajax({
        url: '/rule/source-objects',
        type: 'GET',
        data: { searchText: searchText },
        success: function(sourceObjects) {
            renderSourceSearchResults(resultsContainer, sourceObjects);
        },
        error: function(xhr, status, error) {
            resultsContainer.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-danger">
                        <i class="bi bi-exclamation-triangle me-2"></i>검색 중 오류가 발생했습니다: ${error}
                    </td>
                </tr>
            `;
        }
    });
}

/**
 * 출발지 검색 결과 렌더링
 * @param {HTMLElement} container 결과 컨테이너
 * @param {Array} sourceObjects 출발지 객체 배열
 */
function renderSourceSearchResults(container, sourceObjects) {
    if (!sourceObjects || sourceObjects.length === 0) {
        container.innerHTML = `
            <tr>
                <td colspan="5" class="text-center">검색 결과가 없습니다.</td>
            </tr>
        `;
        return;
    }
    
    let html = '';
    sourceObjects.forEach(obj => {
        html += `
            <tr>
                <td>${formatSourceObjectType(obj.type)}</td>
                <td>${obj.name || ''}</td>
                <td>${obj.ipAddress || ''}</td>
                <td>${obj.zone || ''}</td>
                <td>
                    <button type="button" class="btn btn-sm btn-primary" 
                            onclick="selectSourceObject('${obj.id}', '${obj.type}', '${obj.name}')">
                        선택
                    </button>
                </td>
            </tr>
        `;
    });
    
    container.innerHTML = html;
}

/**
 * 출발지 객체 유형 포맷팅
 * @param {string} type 객체 유형
 * @returns {string} 포맷팅된 유형 텍스트
 */
function formatSourceObjectType(type) {
    switch (type) {
        case 'SERVER':
            return '서버';
        case 'GENERAL':
            return '일반';
        case 'NETWORK':
            return '네트워크';
        default:
            return type;
    }
}

/**
 * 출발지 객체 선택
 * @param {string} id 객체 ID
 * @param {string} type 객체 유형
 * @param {string} name 객체 이름
 */
function selectSourceObject(id, type, name) {
    const prefix = window.currentSourceSearchMode === 'edit' ? 'edit-' : '';
    
    document.getElementById(prefix + 'sourceObjectId').value = id;
    document.getElementById(prefix + 'sourceObjectType').value = type;
    document.getElementById(prefix + 'sourceObjectName').value = name;
    
    // 모달 닫기
    bootstrap.Modal.getInstance(document.getElementById('sourceSearchModal')).hide();
}

/**
 * 폼 유효성 검사 및 제출
 * @param {string} formId 폼 ID
 */
function validateAndSubmit(formId) {
    const form = document.getElementById(formId);
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    // 추가 검증
    if (formId === 'addPolicyForm') {
        // 출발지 선택 확인
        if (!document.getElementById('sourceObjectId').value) {
            alert('출발지 객체를 선택해주세요.');
            return;
        }
        
        // 포트 범위 검증 (Multi 모드인 경우)
        if (document.getElementById('portMode').value === 'multi') {
            const startPort = parseInt(document.getElementById('startPort').value);
            const endPort = parseInt(document.getElementById('endPort').value);
            
            if (isNaN(startPort) || isNaN(endPort) || endPort <= startPort) {
                alert('종료 포트는 시작 포트보다 커야 합니다.');
                return;
            }
        }
    }
    
    // 폼 제출
    form.submit();
}

/**
 * 정책 수정
 * @param {number} id 정책 ID
 */
function editPolicy(id) {
    // Ajax 요청으로 정책 정보 가져오기
    $.ajax({
        url: `/rule/${id}`,
        type: 'GET',
        success: function(policy) {
            // 폼에 정보 설정
            document.getElementById('edit-id').value = policy.id;
            
            // 서버 객체 ID
            $('#edit-serverObjectId').val(policy.serverObjectId).trigger('change');
            document.getElementById('edit-serverObjectId-hidden').value = policy.serverObjectId;
            
            // 우선순위
            document.getElementById('edit-priority').value = policy.priority;
            document.getElementById('edit-priority-hidden').value = policy.priority;
            
            // 출발지 객체
            document.getElementById('edit-sourceObjectName').value = policy.sourceObjectName;
            document.getElementById('edit-sourceObjectId').value = policy.sourceObjectId;
            document.getElementById('edit-sourceObjectType').value = policy.sourceObjectType;
            
            // 프로토콜
            document.getElementById('edit-protocol').value = policy.protocol;
            document.getElementById('edit-protocol-hidden').value = policy.protocol;
            
            // 포트 모드
            document.getElementById('edit-portMode').value = policy.portMode;
            document.getElementById('edit-portMode-hidden').value = policy.portMode;
            
            // 포트
            document.getElementById('edit-startPort').value = policy.startPort;
            document.getElementById('edit-startPort-hidden').value = policy.startPort;
            
            // 포트 범위
            if (policy.portMode === 'multi' && policy.endPort) {
                document.getElementById('edit-endPort').value = policy.endPort;
                document.getElementById('edit-endPort-hidden').value = policy.endPort;
            }
            
            // 동작
            document.getElementById('edit-action').value = policy.action;
            document.getElementById('edit-action-hidden').value = policy.action;
            
            // 시간제한
            if (policy.timeLimit) {
                document.getElementById('edit-timeLimit').value = policy.timeLimit;
                document.getElementById('edit-timeLimit-hidden').value = policy.timeLimit;
            }
            
            // 로깅
            document.getElementById('edit-logging').value = policy.logging.toString();
            document.getElementById('edit-logging-hidden').value = policy.logging;
            
            // 요청자 및 설명 (수정 가능)
            document.getElementById('edit-requester').value = policy.requester || '';
            document.getElementById('edit-description').value = policy.description || '';
            
            // 모달 표시
            const modal = new bootstrap.Modal(document.getElementById('editPolicyModal'));
            modal.show();
        },
        error: function(xhr, status, error) {
            alert('정책 정보를 가져오는데 실패했습니다: ' + error);
        }
    });
}

/**
 * 정책 삭제
 * @param {number} id 정책 ID
 */
function deletePolicy(id) {
    if (confirm('정말 이 정책을 삭제하시겠습니까?')) {
        document.getElementById('deletePolicyId').value = id;
        document.getElementById('deletePolicyForm').submit();
    }
}

/**
 * 모든 서버 패널을 펼치거나 접는 함수
 */
function toggleAllPanels() {
    const btn = document.getElementById('toggleAllBtn');
    const isExpanded = btn.innerHTML.includes('접기');
    const serverPanels = document.querySelectorAll('.server-panel');
    
    if (isExpanded) {
        // 모든 패널 접기
        serverPanels.forEach(panel => {
            if (panel.classList.contains('expanded')) {
                panel.querySelector('.card-header').click();
            }
        });
        btn.innerHTML = '<i class="bi bi-arrows-expand me-1"></i> 전체 펼치기';
    } else {
        // 모든 패널 펼치기
        serverPanels.forEach(panel => {
            if (!panel.classList.contains('expanded')) {
                panel.querySelector('.card-header').click();
            }
        });
        btn.innerHTML = '<i class="bi bi-arrows-collapse me-1"></i> 전체 접기';
    }
}