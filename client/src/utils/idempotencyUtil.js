import {v4 as uuidv4} from "uuid";

export const IDEMPOTENCY_HEADER = "Idempotency-Key"

const SESSION_STORAGE_IDEMPOTENCY_KEY = "idempotency_key"

export const getIdempotencyKey = () => {
    let value = sessionStorage.getItem(SESSION_STORAGE_IDEMPOTENCY_KEY);

    if (!value) {
        value = uuidv4();
        sessionStorage.setItem(SESSION_STORAGE_IDEMPOTENCY_KEY, value);
    }

    return value;
};

export const removeIdempotencyKey = () => {
    sessionStorage.removeItem(SESSION_STORAGE_IDEMPOTENCY_KEY);
}