package com.sneakerhouse.backend.controller;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestControllerAdvice
public class HomepageExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,Object>> badRequest(IllegalArgumentException ex){return ResponseEntity.badRequest().body(Map.of("success",false,"error","VALIDATION_ERROR","message",ex.getMessage()));}
 @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<Map<String,Object>> notFound(NoSuchElementException ex){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success",false,"error","NOT_FOUND","message",ex.getMessage()));}
}
