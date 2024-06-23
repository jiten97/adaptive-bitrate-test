package dev.jiten.hls.abr.exceptionHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDateTime;


@ControllerAdvice
public class ExceptionHandlingControllerAdvice {

	private static final Logger logger = LogManager.getLogger(ExceptionHandlingControllerAdvice.class);

	@ExceptionHandler(CustomException.class)
	@ResponseBody
	public  ResponseEntity<String> handleException(CustomException customException) {
		logger.info("In Custom global exception handler");
		JSONObject body= new JSONObject();
		body.put("errorCode",customException.getExceptionCode());
		body.put("errorMessage",  customException.getExceptionMessage());
		body.put("timeStamp", LocalDateTime.now().toString());
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body.toString());
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public  ResponseEntity<String> handlerExceptions(Exception ex)
	{
		logger.info("In Exception global exception handler");
		JSONObject body= new JSONObject();
		body.put("errorCode",ex.getMessage());
		body.put("errorMessage", 500);
		body.put("timeStamp", LocalDateTime.now().toString());
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body.toString());
	}

}
