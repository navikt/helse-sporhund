package no.nav.helse.sporhund.e2e

import kotlin.test.Test

class DialogmeldingE2ETest {
    @Test
    fun `forespørsel fra saksbehandler sendes til behandler med PDF og journalføres`() =
        e2e {
            når {
                saksbehandlerSenderForespørsel("Kan du sende journalnotat?")
            }
            så {
                behandlerHarMottattDialogmelding("Kan du sende journalnotat?", medVedlegg = true)
                dialogmeldingenErJournalført()
            }
        }

    @Test
    fun `svar fra behandler blir synlig for saksbehandler`() =
        e2e {
            gitt {
                saksbehandlerHarSendtForespørsel("Kan du sende journalnotat?")
            }
            når {
                behandlerSvarer("Her er notatet")
            }
            så {
                saksbehandlerSerSvaret("Her er notatet")
            }
        }

    @Test
    fun `svar fra saksbehandler sendes til behandler med PDF og journalføres`() =
        e2e {
            gitt {
                saksbehandlerHarSendtForespørsel("Kan du sende journalnotat?")
                behandlerHarSvart("Her er notatet")
            }
            når {
                saksbehandlerSvarer("Takk")
            }
            så {
                behandlerHarMottattDialogmelding("Takk", medVedlegg = true)
                dialogmeldingenErJournalført()
            }
        }

    @Test
    fun `saksbehandler kan ferdigstille dialogen etter svar fra behandler`() =
        e2e {
            gitt {
                saksbehandlerHarSendtForespørsel("Kan du sende journalnotat?")
                behandlerHarSvart("Her er notatet")
            }
            når {
                saksbehandlerFerdigstillerDialogen()
            }
            så {
                dialogenErFerdigstilt()
            }
        }
}
