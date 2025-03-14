package com.nicednb.svrgate.exception;

import org.springframework.security.authentication.AccountStatusException;

public class IpAddressRestrictionException extends AccountStatusException {
    public IpAddressRestrictionException(String msg) {
        super(msg);
    }
}