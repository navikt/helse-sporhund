# Sporhund

## Beskrivelse

Formålet med Sporhund er å samle logikk og håndtering av dialogmeldinger med behandler for saksbehandlere av sykepenger.

Sporhund mottar og sender dialogmeldinger via følgende fire kafka topics: #1, #2, #3, og #4.

Sporhund har ansvar for å koble en utgående dialogmelding til en innkommende dialogmelding. 

Sporhund fungerer som backendløsning til Speil for dialogmeldinger. 

Sporhund integrerer med Tilgangsmaskinen for [populasjonstilgangskontroll](https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen#IntrotilTilgangsmaskinen-Slikfungererl%C3%B8sningen).

Sporhund bruker en felles personpseudoid-løsning for å identifisere en person.


## Henvendelser
Sporhund eies og forvaltes av Team SAS (Saksbehandling av Sykepenger). Vi kan nås på slack #sykepenger-værsågod