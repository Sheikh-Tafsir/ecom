import { useSearchParams, useNavigate } from "react-router-dom";

export default function PaymentFail() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const reason   = params.get("reason") || "Payment was not completed.";

  return (
    <div style={{ textAlign: "center", padding: "40px" }}>
      <h1 style={{ color: "#dc3545" }}>❌ Payment Failed</h1>
      <p>{reason}</p>
      <button onClick={() => navigate(-1)}>Try Again</button>
    </div>
  );
}