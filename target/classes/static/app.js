var autoRefreshCount = 0;
var autoRefreshIntervalId = null;

function refreshTimeTable() {
    $.getJSON("/timeTable", function (timeTable) {
        refreshSolvingButtons(timeTable.solverStatus != null && timeTable.solverStatus !== "NOT_SOLVING");
        $("#score").text("Score: "+ (timeTable.score == null ? "?" : timeTable.score));

        const timeTableByShift = $("#timeTableByShift");
        timeTableByShift.children().remove();
        const timeTableByName = $("#timeTableByName");
        timeTableByName.children().remove();
        const timeTableByStaffGroup = $("#timeTableByStaffGroup");
        timeTableByStaffGroup.children().remove();
        const unassignedStaffs = $("#unassignedStaffs");
        unassignedStaffs.children().remove();

        const theadByShift = $("<thead>").appendTo(timeTableByShift);
        const headerRowByShift = $("<tr>").appendTo(theadByShift);
        headerRowByShift.append($("<th>Timeslot</th>"));
        $.each(timeTable.shiftList, (index, shift) => {
            headerRowByShift
                    .append($("<th/>")
                            .append($("<span/>").text(shift.name))
                            .append($(`<button type="button" class="ml-2 mb-1 btn btn-light btn-sm p-1"/>`)
                                    .append($(`<small class="fas fa-trash"/>`)
                                    ).click(() => deleteShift(shift))));
        });
        const theadByName = $("<thead>").appendTo(timeTableByName);
        const headerRowByName = $("<tr>").appendTo(theadByName);
        headerRowByName.append($("<th>Timeslot</th>"));
        const nameList = [...new Set(timeTable.staffList.map(staff => staff.name))];
        $.each(nameList, (index, name) => {
            headerRowByName
                    .append($("<th/>")
                            .append($("<span/>").text(name)));
        });
        const theadByStaffGroup = $("<thead>").appendTo(timeTableByStaffGroup);
        const headerRowByStaffGroup = $("<tr>").appendTo(theadByStaffGroup);
        headerRowByStaffGroup.append($("<th>Timeslot</th>"));
        const staffGroupList = [...new Set(timeTable.staffList.map(staff => staff.staffGroup))];
        $.each(staffGroupList, (index, staffGroup) => {
            headerRowByStaffGroup
                    .append($("<th/>")
                            .append($("<span/>").text(staffGroup)));
        });

        const tbodyByShift = $("<tbody>").appendTo(timeTableByShift);
        const tbodyByName = $("<tbody>").appendTo(timeTableByName);
        const tbodyByStaffGroup = $("<tbody>").appendTo(timeTableByStaffGroup);
        $.each(timeTable.timeslotList, (index, timeslot) => {
            const rowByShift = $("<tr>").appendTo(tbodyByShift);
            rowByShift
                    .append($(`<th class="align-middle"/>`)
                            .append($("<span/>").text(`
                    ${timeslot.dayOfWeek.charAt(0) + timeslot.dayOfWeek.slice(1).toLowerCase()}
                    ${moment(timeslot.startTime, "HH:mm:ss").format("HH:mm")}
                    -
                    ${moment(timeslot.endTime, "HH:mm:ss").format("HH:mm")}
                `)
                                    .append($(`<button type="button" class="ml-2 mb-1 btn btn-light btn-sm p-1"/>`)
                                            .append($(`<small class="fas fa-trash"/>`)
                                            ).click(() => deleteTimeslot(timeslot)))));

            const rowByName = $("<tr>").appendTo(tbodyByName);
            rowByName
                    .append($(`<th class="align-middle"/>`)
                            .append($("<span/>").text(`
                    ${timeslot.dayOfWeek.charAt(0) + timeslot.dayOfWeek.slice(1).toLowerCase()}
                    ${moment(timeslot.startTime, "HH:mm:ss").format("HH:mm")}
                    -
                    ${moment(timeslot.endTime, "HH:mm:ss").format("HH:mm")}
                `)));
            $.each(timeTable.shiftList, (index, shift) => {
                rowByShift.append($("<td/>").prop("id", `timeslot${timeslot.id}shift${shift.id}`));
            });
            const rowByStaffGroup = $("<tr>").appendTo(tbodyByStaffGroup);
            rowByStaffGroup
                    .append($(`<th class="align-middle"/>`)
                            .append($("<span/>").text(`
                    ${timeslot.dayOfWeek.charAt(0) + timeslot.dayOfWeek.slice(1).toLowerCase()}
                    ${moment(timeslot.startTime, "HH:mm:ss").format("HH:mm")}
                    -
                    ${moment(timeslot.endTime, "HH:mm:ss").format("HH:mm")}
                `)));

            $.each(nameList, (index, name) => {
                rowByName.append($("<td/>").prop("id", `timeslot${timeslot.id}name${convertToId(name)}`));
            });

            $.each(staffGroupList, (index, staffGroup) => {
                rowByStaffGroup.append($("<td/>").prop("id", `timeslot${timeslot.id}staffGroup${convertToId(staffGroup)}`));
            });
        });

        $.each(timeTable.staffList, (index, staff) => {
            const color = pickColor(staff.cardID);
            const staffElementWithoutDelete = $(`<div class="card staff" style="background-color: ${color}"/>`)
                    .append($(`<div class="card-body p-2"/>`)
                            .append($(`<h5 class="card-title mb-1"/>`).text(staff.cardID))
                            .append($(`<p class="card-text ml-2 mb-1"/>`)
                                    .append($(`<em/>`).text(`${staff.name}`)))
                            .append($(`<p class="card-text ml-2"/>`).text(staff.staffGroup)));
            const staffElement = staffElementWithoutDelete.clone();
            staffElement.find(".card-body").prepend(
                    $(`<button type="button" class="ml-2 btn btn-light btn-sm p-1 float-right"/>`)
                            .append($(`<small class="fas fa-trash"/>`)
                            ).click(() => deleteStaff(staff))
            );
            if (staff.timeslot == null || staff.shift == null) {
                unassignedStaffs.append(staffElement);
            } else {
                $(`#timeslot${staff.timeslot.id}shift${staff.shift.id}`).append(staffElement);
                $(`#timeslot${staff.timeslot.id}name${convertToId(staff.name)}`).append(staffElementWithoutDelete.clone());
                $(`#timeslot${staff.timeslot.id}staffGroup${convertToId(staff.staffGroup)}`).append(staffElementWithoutDelete.clone());
            }
        });
    });
}

function convertToId(str) {
    // Base64 encoding without padding to avoid XSS
    return btoa(str).replace(/=/g, "");
}

function solve() {
    $.post("/timeTable/solve", function () {
        refreshSolvingButtons(true);
        autoRefreshCount = 16;
        if (autoRefreshIntervalId == null) {
            autoRefreshIntervalId = setInterval(autoRefresh, 2000);
        }
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Start solving failed.", xhr);
    });
}

function refreshSolvingButtons(solving) {
    if (solving) {
        $("#solveButton").hide();
        $("#stopSolvingButton").show();
    } else {
        $("#solveButton").show();
        $("#stopSolvingButton").hide();
    }
}

function autoRefresh() {
    refreshTimeTable();
    autoRefreshCount--;
    if (autoRefreshCount <= 0) {
        clearInterval(autoRefreshIntervalId);
        autoRefreshIntervalId = null;
    }
}

function stopSolving() {
    $.post("/timeTable/stopSolving", function () {
        refreshSolvingButtons(false);
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Stop solving failed.", xhr);
    });
}

function addStaff() {
    var cardID = $("#staff_cardID").val().trim();
    $.post("/staffs", JSON.stringify({
        "cardID": cardID,
        "name": $("#staff_name").val().trim(),
        "staffGroup": $("#staff_staffGroup").val().trim()
    }), function () {
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Adding staff (" + cardID + ") failed.", xhr);
    });
    $('#staffDialog').modal('toggle');
}

function deleteStaff(staff) {
    $.delete("/staffs/" + staff.id, function () {
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Deleting staff (" + staff.name + ") failed.", xhr);
    });
}

function addTimeslot() {
    $.post("/timeslots", JSON.stringify({
        "dayOfWeek": $("#timeslot_dayOfWeek").val().trim().toUpperCase(),
        "startTime": $("#timeslot_startTime").val().trim(),
        "endTime": $("#timeslot_endTime").val().trim()
    }), function () {
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Adding timeslot failed.", xhr);
    });
    $('#timeslotDialog').modal('toggle');
}

function deleteTimeslot(timeslot) {
    $.delete("/timeslots/" + timeslot.id, function () {
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Deleting timeslot (" + timeslot.name + ") failed.", xhr);
    });
}

function addShift() {
    var name = $("#shift_name").val().trim();
    $.post("/shifts", JSON.stringify({
        "name": name
    }), function () {
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Adding shift (" + name + ") failed.", xhr);
    });
    $("#shiftDialog").modal('toggle');
}

function deleteShift(shift) {
    $.delete("/shifts/" + shift.id, function () {
        refreshTimeTable();
    }).fail(function(xhr, ajaxOptions, thrownError) {
        showError("Deleting shift (" + shift.name + ") failed.", xhr);
    });
}

function showError(title, xhr) {
    const serverErrorMessage = !xhr.responseJSON ? `${xhr.status}: ${xhr.statusText}` : xhr.responseJSON.message;
    console.error(title + "\n" + serverErrorMessage);
    const notification = $(`<div class="toast" role="alert" role="alert" aria-live="assertive" aria-atomic="true" style="min-width: 30rem"/>`)
            .append($(`<div class="toast-header bg-danger">
                            <strong class="mr-auto text-dark">Error</strong>
                            <button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>`))
            .append($(`<div class="toast-body"/>`)
                    .append($(`<p/>`).text(title))
                    .append($(`<pre/>`)
                            .append($(`<code/>`).text(serverErrorMessage))
                    )
            );
    $("#notificationPanel").append(notification);
    notification.toast({delay: 30000});
    notification.toast('show');
}

$(document).ready( function() {
    $.ajaxSetup({
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    });
    // Extend jQuery to support $.put() and $.delete()
    jQuery.each( [ "put", "delete" ], function( i, method ) {
        jQuery[method] = function (url, data, callback, type) {
            if (jQuery.isFunction(data)) {
                type = type || callback;
                callback = data;
                data = undefined;
            }
            return jQuery.ajax({
                url: url,
                type: method,
                dataType: type,
                data: data,
                success: callback
            });
        };
    });


    $("#refreshButton").click(function() {
        refreshTimeTable();
    });
    $("#solveButton").click(function() {
        solve();
    });
    $("#stopSolvingButton").click(function() {
        stopSolving();
    });
    $("#addStaffSubmitButton").click(function() {
        addStaff();
    });
    $("#addTimeslotSubmitButton").click(function() {
        addTimeslot();
    });
    $("#addShiftSubmitButton").click(function() {
        addShift();
    });

    refreshTimeTable();
});

// ****************************************************************************
// TangoColorFactory
// ****************************************************************************

const SEQUENCE_1 = [0x8AE234, 0xFCE94F, 0x729FCF, 0xE9B96E, 0xAD7FA8];
const SEQUENCE_2 = [0x73D216, 0xEDD400, 0x3465A4, 0xC17D11, 0x75507B];

var colorMap = new Map;
var nextColorCount = 0;

function pickColor(object) {
    let color = colorMap[object];
    if (color !== undefined) {
        return color;
    }
    color = nextColor();
    colorMap[object] = color;
    return color;
}

function nextColor() {
    let color;
    let colorIndex = nextColorCount % SEQUENCE_1.length;
    let shadeIndex = Math.floor(nextColorCount / SEQUENCE_1.length);
    if (shadeIndex === 0) {
        color = SEQUENCE_1[colorIndex];
    } else if (shadeIndex === 1) {
        color = SEQUENCE_2[colorIndex];
    } else {
        shadeIndex -= 3;
        let floorColor = SEQUENCE_2[colorIndex];
        let ceilColor = SEQUENCE_1[colorIndex];
        let base = Math.floor((shadeIndex / 2) + 1);
        let divisor = 2;
        while (base >= divisor) {
            divisor *= 2;
        }
        base = (base * 2) - divisor + 1;
        let shadePercentage = base / divisor;
        color = buildPercentageColor(floorColor, ceilColor, shadePercentage);
    }
    nextColorCount++;
    return "#" + color.toString(16);
}

function buildPercentageColor(floorColor, ceilColor, shadePercentage) {
    let red = (floorColor & 0xFF0000) + Math.floor(shadePercentage * ((ceilColor & 0xFF0000) - (floorColor & 0xFF0000))) & 0xFF0000;
    let green = (floorColor & 0x00FF00) + Math.floor(shadePercentage * ((ceilColor & 0x00FF00) - (floorColor & 0x00FF00))) & 0x00FF00;
    let blue = (floorColor & 0x0000FF) + Math.floor(shadePercentage * ((ceilColor & 0x0000FF) - (floorColor & 0x0000FF))) & 0x0000FF;
    return red | green | blue;
}
