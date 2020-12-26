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
    preloadDim();

    window.setInterval(getStatus, 1000);
}

var previousStatusText = '';

function getStatus() {
    if (isGetStatusPending) {
        logGetStatus('already pending, skipping...');
        return;
    }
    logGetStatus('=> pending', true);
    var req = getGetStatusXMLHttpRequest();
    try {
        req.open('GET', '?cmd=status', true);
    } catch (e) {
        logGetStatus('=> open failed', false);
        return;
    }
    req.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    try {
        req.send();
    } catch (e) {
        logGetStatus('=> send failed', false);
    }
}

function getGetStatusXMLHttpRequest() {
    var req = new XMLHttpRequest();
    req.onreadystatechange = function() {
        if (isLogGetStatus) {
            logGetStatus('=> readyState ' + req.readyState + ' (' + getReadyStateName(req.readyState) + ')')
        }
        if (req.readyState === 4) {
            try {
                var text = req.responseText;
                if (req.status / 100 !== 2) {
                    text = ''
                }
                if (text === '') {
                    text = previousStatusText;
                    if (text.indexOf('(offline)') < 0) {
                        text = text.replace('<strong>Jexlers</strong>', '<strong>(offline)</strong>');
                        text = text.replace(/\.gif'/g, '-dim.gif');
                        text = text.replace(/\.gif"/g, '-dim.gif"');
                        text = text.replace(/<a href=.\?cmd=[a-z]+&jexler=[A-Za-z0-9]*.>/g, '');
                        text = text.replace(/<\/a>/g, '');
                        text = text.replace(/<button.* formaction=.\?jexler=[A-Za-z0-9]*.>/g, '');
                        text = text.replace(/<\/button>/g, '');
                        text = text.replace(/status-name/g, 'status-name status-offline');
                    }
                }
                if (text !== previousStatusText) {
                    previousStatusText = text;
                    var statusDiv = document.getElementById('statusdiv');
                    statusDiv.innerHTML = text;
                }
            } finally {
                logGetStatus('=> finally', false);
            }
        }
    };
    req.onabort = function() { logGetStatus('=> aborted', false); };
    req.onerror = function() { logGetStatus('=> error', false); };
    req.onload = function() { logGetStatus('=> loaded', false); };
    req.ontimeout = function() { logGetStatus('=> timeout', false); };
    req.timeout = 5000;
    return req;
}

function getReadyStateName(state) {
    switch (state) {
        case 0: return 'UNSENT';
        case 1: return 'OPENED';
        case 2: return 'HEADERS_RECEIVED';
        case 3: return 'LOADING';
        case 4: return 'DONE';
        default: return '?????';
    }

}

function logGetStatus(info, isPending) {
    if (isLogGetStatus) {
        console.log('getStatus (pending ' + isGetStatusPending + (isPending == null ? '' : '=>' + isPending) + ')  ' +info);
    }
    if (isPending != null) {
        isGetStatusPending = isPending
    }
}

function updateSaveIndicator(jexlerId) {
    currentSource = editor.getValue();
    hasSourceChanged = (savedSource !== currentSource);
    hasJexlerChanged = jexlerId !== document.getElementById('newjexlername').value;
    if (hasJexlerChanged) {
        document.getElementById('savestatus').setAttribute('src', 'ok.gif')
    } else if (hasSourceChanged) {
        document.getElementById('savestatus').setAttribute('src', 'log.gif')
    } else {
        document.getElementById('savestatus').setAttribute('src', 'space.gif')
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
            if (xmlhttp.status / 100 === 2 && xmlhttp.responseText !== '') {
                editor.focus();
                savedSource = currentSource;
                hasSourceChanged = false;
                document.getElementById('savestatus').setAttribute('src', 'space.gif')
            }
        }
    };
    xmlhttp.open('POST', '?cmd=save&jexler=' + jexlerId, true);
    xmlhttp.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    xmlhttp.setRequestHeader('Content-type','application/x-www-form-urlencoded; charset=utf-8');
    xmlhttp.timeout = 5000;
    xmlhttp.send('source=' + encodeURIComponent(currentSource));
    return false;
}

function isPostDelete(confirmDelete, jexlerId) {
    console.log('confirmDelete: ' + confirmDelete);
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
    document.getElementById('sourcediv').style.height = '' + h + 'px';
    document.getElementById('statusdiv').style.height = '' + h + 'px';
}

function preloadDim() {
    new Image().src = 'error-dim.gif';
    new Image().src = 'info-dim.gif';
    new Image().src = 'log-dim.gif';
    new Image().src = 'neutral-dim.gif';
    new Image().src = 'ok-dim.gif';
    new Image().src = 'powered-by-grengine-dim.gif';
    new Image().src = 'restart-dim.gif';
    new Image().src = 'space-dim.gif';
    new Image().src = 'start-dim.gif';
    new Image().src = 'stop-dim.gif';
    new Image().src = 'web-dim.gif';
    new Image().src = 'wheel-dim.gif';
    new Image().src = 'zap-dim.gif';
}