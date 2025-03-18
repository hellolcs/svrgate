// Zone 페이지 관련 자바스크립트
document.addEventListener('DOMContentLoaded', function() {
    initializeZonePage();
});

// 재귀 호출 방지를 위한 플래그
let isUpdating = false;
let isEditUpdating = false;

function initializeZonePage() {
    // 제이쿼리가 로드되었는지 확인
    if (typeof jQuery === 'undefined') {
        console.error('jQuery is not loaded');
        return;
    }
    
    // Select2 초기화
    if (typeof $.fn.select2 !== 'undefined') {
        initializeSelect2();
        setupEventHandlers();
    } else {
        console.error('Select2 is not loaded');
    }
}

function initializeSelect2() {
    $('.select2').select2({
        theme: 'bootstrap-5',
        width: '100%',
        placeholder: '선택하세요',
        allowClear: true
    });
}

function setupEventHandlers() {
    // 비보안Zone 선택 시 보안Zone에서 해당 Zone 제외
    $('#nonSecureZoneIds').on('change', function() {
        if (isUpdating) return; // 재귀 호출 방지
        
        isUpdating = true;
        try {
            let nonSecureSelected = $(this).val() || [];
            
            // 비보안Zone에 선택된 항목들을 보안Zone에서 비활성화
            $('#secureZoneIds option').each(function() {
                if(nonSecureSelected.includes($(this).val())) {
                    $(this).prop('disabled', true);
                } else {
                    $(this).prop('disabled', false);
                }
            });
            
            // 현재 보안Zone 선택 항목 가져오기
            let secureSelected = $('#secureZoneIds').val() || [];
            
            // 중복 제거 (비보안Zone에서 선택된 항목은 보안Zone에서 제거)
            secureSelected = secureSelected.filter(id => !nonSecureSelected.includes(id));
            
            // 값만 변경하고 change 이벤트는 발생시키지 않음
            $('#secureZoneIds').val(secureSelected);
            
            // Select2 UI 업데이트
            $('#secureZoneIds').select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: '선택하세요',
                allowClear: true
            });
        } finally {
            isUpdating = false;
        }
    });
    
    // 보안Zone 선택 시 비보안Zone에서 해당 Zone 제외
    $('#secureZoneIds').on('change', function() {
        if (isUpdating) return; // 재귀 호출 방지
        
        isUpdating = true;
        try {
            let secureSelected = $(this).val() || [];
            
            // 보안Zone에 선택된 항목들을 비보안Zone에서 비활성화
            $('#nonSecureZoneIds option').each(function() {
                if(secureSelected.includes($(this).val())) {
                    $(this).prop('disabled', true);
                } else {
                    $(this).prop('disabled', false);
                }
            });
            
            // 현재 비보안Zone 선택 항목 가져오기
            let nonSecureSelected = $('#nonSecureZoneIds').val() || [];
            
            // 중복 제거 (보안Zone에서 선택된 항목은 비보안Zone에서 제거)
            nonSecureSelected = nonSecureSelected.filter(id => !secureSelected.includes(id));
            
            // 값만 변경하고 change 이벤트는 발생시키지 않음
            $('#nonSecureZoneIds').val(nonSecureSelected);
            
            // Select2 UI 업데이트
            $('#nonSecureZoneIds').select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: '선택하세요',
                allowClear: true
            });
        } finally {
            isUpdating = false;
        }
    });
    
    // 수정 모달에서도 동일한 로직 적용
    $('#edit-nonSecureZoneIds').on('change', function() {
        if (isEditUpdating) return;
        
        isEditUpdating = true;
        try {
            let nonSecureSelected = $(this).val() || [];
            
            // 비보안Zone에 선택된 항목들을 보안Zone에서 비활성화
            $('#edit-secureZoneIds option').each(function() {
                if(nonSecureSelected.includes($(this).val())) {
                    $(this).prop('disabled', true);
                } else {
                    // 자기 자신은 항상 비활성화
                    if($(this).val() === $('#edit-id').val()) {
                        $(this).prop('disabled', true);
                    } else {
                        $(this).prop('disabled', false);
                    }
                }
            });
            
            let secureSelected = $('#edit-secureZoneIds').val() || [];
            secureSelected = secureSelected.filter(id => !nonSecureSelected.includes(id));
            $('#edit-secureZoneIds').val(secureSelected);
            
            // Select2 UI 업데이트
            $('#edit-secureZoneIds').select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: '선택하세요',
                allowClear: true
            });
        } finally {
            isEditUpdating = false;
        }
    });
    
    $('#edit-secureZoneIds').on('change', function() {
        if (isEditUpdating) return;
        
        isEditUpdating = true;
        try {
            let secureSelected = $(this).val() || [];
            
            // 보안Zone에 선택된 항목들을 비보안Zone에서 비활성화
            $('#edit-nonSecureZoneIds option').each(function() {
                if(secureSelected.includes($(this).val())) {
                    $(this).prop('disabled', true);
                } else {
                    // 자기 자신은 항상 비활성화
                    if($(this).val() === $('#edit-id').val()) {
                        $(this).prop('disabled', true);
                    } else {
                        $(this).prop('disabled', false);
                    }
                }
            });
            
            let nonSecureSelected = $('#edit-nonSecureZoneIds').val() || [];
            nonSecureSelected = nonSecureSelected.filter(id => !secureSelected.includes(id));
            $('#edit-nonSecureZoneIds').val(nonSecureSelected);
            
            // Select2 UI 업데이트
            $('#edit-nonSecureZoneIds').select2({
                theme: 'bootstrap-5',
                width: '100%',
                placeholder: '선택하세요',
                allowClear: true
            });
        } finally {
            isEditUpdating = false;
        }
    });
}

// Zone 수정 모달 열기
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
            
            // Select2 초기화
            $('#edit-nonSecureZoneIds').empty();
            $('#edit-secureZoneIds').empty();
            
            // 활성화된 모든 Zone을 옵션으로 추가 (편집 중인 Zone 제외)
            const activeZonesElement = document.getElementById('activeZonesData');
            if (activeZonesElement) {
                try {
                    const activeZones = JSON.parse(activeZonesElement.textContent);
                    activeZones.forEach(function(zone) {
                        // 자기 자신은 제외
                        if (zone.id != response.id) {
                            let nonSecureOption = new Option(zone.name, zone.id, false, false);
                            let secureOption = new Option(zone.name, zone.id, false, false);
                            $('#edit-nonSecureZoneIds').append(nonSecureOption);
                            $('#edit-secureZoneIds').append(secureOption);
                        }
                    });
                } catch (e) {
                    console.error('Failed to parse activeZones JSON:', e);
                }
            }
            
            // 비보안Zone 및 보안Zone 값 설정
            if (response.nonSecureZoneIds && response.nonSecureZoneIds.length > 0) {
                $('#edit-nonSecureZoneIds').val(response.nonSecureZoneIds);
            }
            
            if (response.secureZoneIds && response.secureZoneIds.length > 0) {
                $('#edit-secureZoneIds').val(response.secureZoneIds);
            }
            
            // Select2 초기화
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
            
            // 모달 열기
            $('#editZoneModal').modal('show');
            
            // 보안/비보안 Zone 선택 상태 업데이트 (비활성화 처리)
            let nonSecureSelected = $('#edit-nonSecureZoneIds').val() || [];
            let secureSelected = $('#edit-secureZoneIds').val() || [];
            
            // 비보안Zone에 선택된 항목들을 보안Zone에서 비활성화
            $('#edit-secureZoneIds option').each(function() {
                if(nonSecureSelected.includes($(this).val())) {
                    $(this).prop('disabled', true);
                }
            });
            
            // 보안Zone에 선택된 항목들을 비보안Zone에서 비활성화
            $('#edit-nonSecureZoneIds option').each(function() {
                if(secureSelected.includes($(this).val())) {
                    $(this).prop('disabled', true);
                }
            });
        },
        error: function(xhr, status, error) {
            alert('Zone 정보를 가져오는 중 오류가 발생했습니다: ' + error);
        }
    });
}

// Zone 삭제
function deleteZone(id) {
    if (confirm('정말 이 Zone을 삭제하시겠습니까?')) {
        $('#deleteZoneId').val(id);
        $('#deleteZoneForm').submit();
    }
}

// Excel 업로드 모달 열기
function openExcelUploadModal() {
    $('#excelUploadModal').modal('show');
}