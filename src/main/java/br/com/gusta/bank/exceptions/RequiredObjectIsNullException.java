package br.com.gusta.bank.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RequiredObjectIsNullException extends RuntimeException {

	public RequiredObjectIsNullException(String str) {
		super(str);
	}
	public RequiredObjectIsNullException() {
		super("It is not allowed to persist a null object!");
	}
}
