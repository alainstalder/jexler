// Default settings for jexler web GUI (ConfigSlurper).
// Can be individually overridden in settings-custom.groovy.

operation {
    jexler {
        // Timeout in seconds for starting a jexler before reporting an issue.
        startTimeoutSecs = 10
        // Timeout in seconds for stopping a jexler before reporting an issue.
        stopTimeoutSecs = 10
    }
}

security {
    script {
        // Whether to allow editing jexler scripts in web GUI or not.
        allowEdit = true
    }
}

safety {
    script {
        // Whether to confirm script save in web GUI or not.
        confirmSave = false
        // Whether to confirm script delete in web GUI or not.
        confirmDelete = true
    }
}

rest {
    // Return jexler ID from HttpServletRequest;
    // script binding passes 'httpReq' and 'log'.
    idGetter = '''
        String jexlerId = httpReq.getHeader('jexler')
        if (jexlerId == null) {
          log.error("Missing header 'jexler'.")
        }
        return jexlerId
        '''
    // Send error HTTP response; script binding passes 'httpReq'
    // and 'httpResp', with the httpReq.status already set.
    // The default does nothing more, resulting in an empty response
    // body (unless already committed by a failing jexler script).
    errorSender = ''
}
