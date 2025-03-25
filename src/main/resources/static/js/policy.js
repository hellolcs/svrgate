// policy.js - 정책관리 관련 자바스크립트

// 현재 편집/선택 중인 모달 모드 (add 또는 edit)
let currentModalMode = 'add';

// DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function() {
    initializePolicyPage();
});

/**
 * 정책관리 페이지 초기화
 */
function initializePolicyPage() {
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
 * 서버 패널 접기/펼치기
 * @param {HTMLElement} element 클릭된 패널 헤더 요소
 */
function toggleServerPanel(element) {
    const panel = element.closest('.server-panel');
    const serverId = panel.getAttribute('data-server-id');
    const isExpanded = panel.classList.contains('expanded');
    const toggleIcon = panel.querySelector('.toggle-icon');
    
    if (isExpanded) {
        // 패널 접기
        panel.classList.remove('expanded');
        toggleIcon.classList.remove('bi-chevron-down');
        toggleIcon.classList.add('bi-chevron-right');
    } else {
        // 패널 펼치기
        panel.classList.add('expanded');
        toggleIcon.classList.remove('bi-chevron-right');
        toggleIcon.classList.add('bi-chevron-down');
        
        // 정책 데이터 로드 (이미 로드되지 않은 경우)
        const tableBody = panel.querySelector(`#policy-table-${serverId} tbody`);
        if (tableBody.innerHTML.includes('Loading')) {
            loadPoliciesByServerId(serverId);
        }
    }
}

/**
 * 서버별 정책 목록 로드
 * @param {string} serverId 서버 ID
 */
function loadPoliciesByServerId(serverId) {
    $.ajax({
        url: `/rule/server/${serverId}`,
        type: 'GET',
        success: function(policies) {
            renderPolicyTable(serverId, policies);
        },
        error: function(xhr, status, error) {
            const tableBody = document.querySelector(`#policy-table-${serverId} tbody`);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="13" class="text-center text-danger">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        정책 목록을 불러오는 중 오류가 발생했습니다: ${error}
                    </td>
                </tr>
            `;
        }
    });
}

/**
 * 정책 테이블 렌더링
 * @param {string} serverId 서버 ID
 * @param {Array} policies 정책 목록
 */
function renderPolicyTable(serverId, policies) {
    const tableBody = document.querySelector(`#policy-table-${serverId} tbody`);
    let html = '';
    
    if (policies.length === 0) {
        html = `
            <tr>
                <td colspan="13" class="text-center">
                    <i class="bi bi-info-circle me-2"></i>
                    이 서버에 등록된 정책이 없습니다.
                </td>
            </tr>
        `;
    } else {
        policies.forEach(policy => {
            html += `
                <tr data-policy-id="${policy.id}">
                    <td>${policy.priority}</td>
                    <td>${policy.sourceObjectName}</td>
                    <td>${policy.protocol.toUpperCase()}</td>
                    <td>${capitalizeFirstLetter(policy.portMode)}</td>
                    <td>${policy.port}</td>
                    <td>${capitalizeFirstLetter(policy.action)}</td>
                    <td>${policy.timeLimit !== null ? policy.timeLimit : '-'}</td>
                    <td>${policy.logging ? '사용' : '미사용'}</td>
                    <td>${policy.registrationDateFormatted}</td>
                    <td>${policy.requester}</td>
                    <td>${policy.registrar}</td>
                    <td>${policy.description || '-'}</td>
                    <td>
                        <button type="button" class="btn btn-outline-primary btn-sm btn-action" 
                                onclick="editPolicy(${policy.id})">수정</button>
                        <button type="button" class="btn btn-outline-danger btn-sm btn-action" 
                                onclick="deletePolicy(${policy.id})">삭제</button>
                    </td>
                </tr>
            `;
        });
    }
    
    tableBody.innerHTML = html;
}

/**
 * 정책 추가 모달 보기
 */
function showAddPolicyModal() {
    // 폼 초기화
    document.getElementById('addPolicyForm').reset();
    document.getElementById('sourceObjectName').value = '';
    document.getElementById('sourceObjectId').value = '';
    document.getElementById('sourceObjectType').value = '';
    
    // Select2 초기화
    $('#serverObjectId').val(null).trigger('change');
    
    // 모달 표시
    $('#addPolicyModal').modal('show');
}

/**
 * 정책 수정
 * @param {number} policyId 정책 ID
 */
function editPolicy(policyId) {
    // 정책 정보 가져오기
    $.ajax({
        url: `/rule/${policyId}`,
        type: 'GET',
        success: function(policy) {
            // 폼에 데이터 설정
            document.getElementById('edit-id').value = policy.id;
            document.getElementById('edit-serverObjectId-hidden').value = policy.serverObjectId;
            document.getElementById('edit-priority').value = policy.priority;
            document.getElementById('edit-sourceObjectId').value = policy.sourceObjectId;
            document.getElementById('edit-sourceObjectType').value = policy.sourceObjectType;
            document.getElementById('edit-sourceObjectName').value = policy.sourceObjectName;
            document.getElementById('edit-protocol').value = policy.protocol;
            document.getElementById('edit-portMode').value = policy.portMode;
            document.getElementById('edit-port').value = policy.port;
            document.getElementById('edit-action').value = policy.action;
            document.getElementById('edit-timeLimit').value = policy.timeLimit !== null ? policy.timeLimit : '';
            document.getElementById('edit-logging').value = policy.logging.toString();
            document.getElementById('edit-requester').value = policy.requester;
            document.getElementById('edit-description').value = policy.description || '';
            
            // Select2 초기화
            $('#edit-serverObjectId').val(policy.serverObjectId).trigger('change');
            
            // 모달 표시
            $('#editPolicyModal').modal('show');
        },
        error: function(xhr, status, error) {
            alert(`정책 정보를 가져오는 중 오류가 발생했습니다: ${error}`);
        }
    });
}

/**
 * 정책 삭제
 * @param {number} policyId 정책 ID
 */
function deletePolicy(policyId) {
    if (confirm('정말 이 정책을 삭제하시겠습니까?')) {
        document.getElementById('deletePolicyId').value = policyId;
        document.getElementById('deletePolicyForm').submit();
    }
}

/**
 * 출발지 검색 모달 열기
 * @param {string} mode 모달 모드 ('add' 또는 'edit')
 */
function openSourceSearchModal(mode) {
    currentModalMode = mode;
    
    // 검색 결과 초기화
    document.getElementById('sourceSearchInput').value = '';
    document.getElementById('sourceSearchResults').innerHTML = '<tr><td colspan="5" class="text-center">검색어를 입력하고 검색 버튼을 클릭하세요.</td></tr>';
    
    // 모달 표시
    $('#sourceSearchModal').modal('show');
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
    
    // 출발지 객체 검색 API 호출
    $.ajax({
        url: '/rule/source-objects',
        type: 'GET',
        data: { searchText: searchText },
        success: function(objects) {
            renderSourceObjects(objects);
        },
        error: function(xhr, status, error) {
            resultsContainer.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-danger">
                        <i class="bi bi-exclamation-triangle me-2"></i>
                        출발지 객체 검색 중 오류가 발생했습니다: ${error}
                    </td>
                </tr>
            `;
        }
    });
}

/**
 * 출발지 객체 목록 렌더링
 * @param {Array} objects 출발지 객체 목록
 */
function renderSourceObjects(objects) {
    const resultsContainer = document.getElementById('sourceSearchResults');
    let html = '';
    
    if (objects.length === 0) {
        html = `
            <tr>
                <td colspan="5" class="text-center">
                    <i class="bi bi-info-circle me-2"></i>
                    검색 결과가 없습니다.
                </td>
            </tr>
        `;
    } else {
        objects.forEach(obj => {
            const type = getObjectTypeDisplay(obj.type);
            
            html += `
                <tr>
                    <td>${type}</td>
                    <td>${obj.name}</td>
                    <td>${obj.ipAddress}</td>
                    <td>${obj.zone || '-'}</td>
                    <td>
                        <button type="button" class="btn btn-sm btn-primary" 
                                onclick="selectSourceObject('${obj.id}', '${obj.type}', '${obj.name}')">
                            선택
                        </button>
                    </td>
                </tr>
            `;
        });
    }
    
    resultsContainer.innerHTML = html;
}

/**
 * 출발지 객체 유형 표시명 조회
 * @param {string} type 객체 유형 ('SERVER', 'GENERAL', 'NETWORK')
 * @returns {string} 표시용 유형명
 */
function getObjectTypeDisplay(type) {
    switch (type) {
        case 'SERVER': return '연동서버';
        case 'GENERAL': return '일반객체';
        case 'NETWORK': return '네트워크객체';
        default: return type;
    }
}

/**
 * 출발지 객체 선택
 * @param {string} id 객체 ID
 * @param {string} type 객체 유형
 * @param {string} name 객체 이름
 */
function selectSourceObject(id, type, name) {
    if (currentModalMode === 'add') {
        document.getElementById('sourceObjectId').value = id;
        document.getElementById('sourceObjectType').value = type;
        document.getElementById('sourceObjectName').value = name;
    } else {
        document.getElementById('edit-sourceObjectId').value = id;
        document.getElementById('edit-sourceObjectType').value = type;
        document.getElementById('edit-sourceObjectName').value = name;
    }
    
    // 검색 모달 닫기
    $('#sourceSearchModal').modal('hide');
}

/**
 * 문자열의 첫 글자를 대문자로 변환
 * @param {string} string 변환할 문자열
 * @returns {string} 첫 글자가 대문자인 문자열
 */
function capitalizeFirstLetter(string) {
    if (!string) return '';
    return string.charAt(0).toUpperCase() + string.slice(1);
}