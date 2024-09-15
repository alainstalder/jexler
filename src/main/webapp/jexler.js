var savedSource;
var currentSource;
var hasSourceChanged;
var hasJexlerChanged;
var isGetStatusPending;
var isLogGetStatus;

function onPageLoad() {
    sourceElement = document.getElementById('source');
    if (sourceElement != null) {
        savedSource = sourceElement.value
    }
    currentSource = savedSource;
    hasSourceChanged = false;
    hasJexlerChanged = false;
    setHeight();
    isGetStatusPending = false;
    isLogGetStatus = false;
    window.setInterval(getStatus, 1000);
    preloadDim();
}

var previousStatusText = "";

function getStatus() {
    if (isGetStatusPending) {
        logGetStatus('skipping');
        return;
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function() {
        if (xmlhttp.readyState === 4) {
            try {
                logGetStatus('=> readyState 4');
                var text = xmlhttp.responseText;
                if (xmlhttp.status / 100 != 2) {
                    text = ""
                }
                if (text == "") {
                    text = previousStatusText;
                    if (text.indexOf("(offline)") < 0) {
                        text = text.replace("<strong>Jexlers</strong>", "<strong>(offline)</strong>");
                        text = text.replace(/\.gif'/g, "-dim.gif'");
                        text = text.replace(/\.gif"/g, "-dim.gif\"");
                        text = text.replace(/<a href=.\?cmd=[a-z]+&jexler=[A-Za-z0-9]*.>/g, "");
                        text = text.replace(/<\/a>/g, "");
                        text = text.replace(/<button.* formaction=.\?jexler=[A-Za-z0-9]*.>/g, "");
                        text = text.replace(/<\/button>/g, "");
                        text = text.replace(/status-name/g, "status-name status-offline");
                    }
                }
                if (text != previousStatusText) {
                    previousStatusText = text;
                    var statusDiv = document.getElementById("statusdiv");
                    statusDiv.innerHTML = text;
                }
            } finally {
                logGetStatus('=> finally');
                isGetStatusPending = false;
            }
        }
    };
    xmlhttp.onabort = function() {
        logGetStatus('=> aborted');
        isGetStatusPending = false;
    };
    xmlhttp.onerror = function() {
        logGetStatus('=> error');
        isGetStatusPending = false;
    };
    xmlhttp.onload = function() {
        logGetStatus('=> loaded');
        isGetStatusPending = false;
    };
    xmlhttp.ontimeout = function() {
        logGetStatus('=> timeout');
        isGetStatusPending = false;
    };
    xmlhttp.open('GET', '?cmd=status', true);
    xmlhttp.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    xmlhttp.timeout = 5000;
    logGetStatus('pending...');
    isGetStatusPending = true;
    xmlhttp.send(null);
}

function logGetStatus(info) {
    if (isLogGetStatus) {
        console.log(info);
    }
}

function updateSaveIndicator(jexlerId) {
    currentSource = editor.getValue();
    hasSourceChanged = (savedSource != currentSource);
    hasJexlerChanged = jexlerId != document.getElementById('newjexlername').value;
    if (hasJexlerChanged) {
        document.getElementById('savestatus').setAttribute("src", "ok.gif")
    } else if (hasSourceChanged) {
        document.getElementById('savestatus').setAttribute("src", "log.gif")
    } else {
        document.getElementById('savestatus').setAttribute("src", "space.gif")
    }
}

function isPostSave(confirmSave, jexlerId) {
    if (confirmSave) {
        if (!confirm("Are you sure you want to save '" + jexlerId + "'?")) {
            return false;
        }
    }
    if (hasJexlerChanged) {
        return true;
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function() {
        if (xmlhttp.readyState === 4) {
            if (xmlhttp.status / 100 == 2 && xmlhttp.responseText != "") {
                editor.focus();
                savedSource = currentSource;
                hasSourceChanged = false;
                document.getElementById('savestatus').setAttribute("src", "space.gif")
            }
        }
    };
    xmlhttp.open('POST', '?cmd=save&jexler=' + jexlerId, true);
    xmlhttp.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded; charset=utf-8");
    xmlhttp.timeout = 5000;
    xmlhttp.send("source=" + encodeURIComponent(currentSource));
    return false;
}

function isPostDelete(confirmDelete, jexlerId) {
    console.log("confirmDelete: " + confirmDelete);
    if (confirmDelete) {
        return confirm("Are you sure you want to delete '" + jexlerId + "'?");
    } else {
        return true;
    }
}

window.onresize = function() {
    setHeight();
};

function setHeight() {
    var hTotal = document.documentElement.clientHeight;
    var hHeader = document.getElementById('header').offsetHeight;
    var h = hTotal - hHeader - 50;
    document.getElementById('sourcediv').style.height = "" + h + "px";
    document.getElementById('statusdiv').style.height = "" + h + "px";
}

function preloadDim() {
    new Image().src = "error-dim.gif";
    new Image().src = "info-dim.gif";
    new Image().src = "log-dim.gif";
    new Image().src = "neutral-dim.gif";
    new Image().src = "ok-dim.gif";
    new Image().src = "powered-by-grengine-dim.gif";
    new Image().src = "restart-dim.gif";
    new Image().src = "space-dim.gif";
    new Image().src = "start-dim.gif";
    new Image().src = "stop-dim.gif";
    new Image().src = "web-dim.gif";
    new Image().src = "wheel-dim.gif";
    new Image().src = "zap-dim.gif";
}