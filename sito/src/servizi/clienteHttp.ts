import axios, { AxiosError } from "axios";
import { RispostaErrore } from "@/tipi/api";

// ============================================================
//  Istanza axios verso il backend authenticator.
//
//  Questo sito non ha un backend suo: il vhost del sottodominio
//  gira /api/ su 127.0.0.1:3002, cioe' sullo stesso backend che
//  serve il pannello admin di teo.place/authenticator/.
//
//  Nessun interceptor di autenticazione: qui si usano SOLO gli
//  endpoint pubblici (/vetrina, /registrazione). Il token admin
//  non deve nemmeno essere letto da un sito pubblico.
// ============================================================

export const clienteHttp = axios.create({
  baseURL: "/api",
});

// Estrae il messaggio di errore in italiano da qualsiasi errore axios.
export function messaggioErrore(errore: unknown): string {
  if (axios.isAxiosError<RispostaErrore>(errore) && errore.response?.data?.errore) {
    return errore.response.data.errore;
  }

  return "Errore di comunicazione con il server";
}

export type { AxiosError };
