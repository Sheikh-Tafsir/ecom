import { useState } from "react";
import { createBkashPayment } from "./bkashService";
import { API_PATH, Axios } from "@/services/http/Axios";

export default function PaymentButton({ userId, amount }) {
  const [isPageLoading, setIsPageLoading] = useState(false);
  const [error, setError]     = useState(null);

  const handlePayment = async () => {
    setIsPageLoading(true);
    setError(null);
    try {
      const response = await Axios.post(`/payment/create`,{
        userId,
        amount
      })
      // Redirect user to bKash-hosted payment page
      window.location.href = response.data.data.bkashURL;
    } catch (err) {
      setError("Payment initiation failed. Please try again.");
    } finally {
      setIsPageLoading(false);
    }
  };

  return (
    <div>
      <button
        onClick={handlePayment}
        disabled={isPageLoading}
        style={{
          backgroundColor: "#E2136E",
          color: "#fff",
          padding: "12px 24px",
          border: "none",
          borderRadius: "6px",
          fontSize: "16px",
          cursor: isPageLoading ? "not-allowed" : "pointer",
        }}
      >
        {isPageLoading ? "Redirecting to bKash..." : `Pay ৳${amount} with bKash`}
      </button>
      {error && <p style={{ color: "red", marginTop: "8px" }}>{error}</p>}
    </div>
  );
}