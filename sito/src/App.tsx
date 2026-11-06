import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { PaginaProdotto } from "@/pagine/PaginaProdotto";

// Sito di un programma di lavoro: una pagina sola, al root del sottodominio. Niente account,
// niente conferma email, niente notifiche (non c'e' nessuna azione che ne produca).
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<PaginaProdotto />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
