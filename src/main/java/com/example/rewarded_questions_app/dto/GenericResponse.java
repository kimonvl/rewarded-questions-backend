package com.example.rewarded_questions_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper used for all API responses.
 *
 * <p>Provides a uniform way of sending the responses in every API.
 * Includes the payload actual data, a response message for the user,
 * and a success flag indicating whether the request was processed successfully.</p>
 *
 * @param <T> the type of the response payload
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse<T> {
    /** Payload data returned by the response */
    private T data;
    /** Code representing the type of response, useful for client-side handling */
    private String code;
    /** Server message */
    private String message;
    /** Flag indicating whether the operation was successful */
    private boolean success;
}