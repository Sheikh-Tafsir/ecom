import { useSearchParams } from "react-router-dom";

export default function PaymentSuccess() {
  const [params] = useSearchParams();
  const paymentID = params.get("paymentID");
  const trxID     = params.get("trxID");
  const amount    = params.get("amount");

  return (
    <div style={{ textAlign: "center", padding: "40px" }}>
      <h1 style={{ color: "#28a745" }}>✅ Payment Successful!</h1>
      <table style={{ margin: "0 auto", borderCollapse: "collapse" }}>
        <tbody>
          <tr><td><b>Payment ID</b></td><td>{paymentID}</td></tr>
          <tr><td><b>Transaction ID</b></td><td>{trxID}</td></tr>
          <tr><td><b>Amount</b></td><td>৳{amount}</td></tr>
        </tbody>
      </table>
    </div>
  );
}