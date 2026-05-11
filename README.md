# Sporhund

## Beskrivelse

Formålet med Sporhund er å samle logikk og håndtering av dialogmeldinger med behandler for saksbehandlere av sykepenger.

Sporhund mottar og sender dialogmeldinger via kafka, se [Topics](#Topics).

Sporhund har ansvar for å koble en utgående dialogmelding til en innkommende dialogmelding. 

Sporhund fungerer som backendløsning til Speil for dialogmeldinger.

Sporhund integrerer med Tilgangsmaskinen for [populasjonstilgangskontroll](https://confluence.adeo.no/spaces/TM/pages/628888614/Intro+til+Tilgangsmaskinen#IntrotilTilgangsmaskinen-Slikfungererl%C3%B8sningen).

Sporhund bruker en felles personpseudoid-løsning for å identifisere en person.

## Teknisk

### Topics

Sporhund produserer dialogmeldinger til behandler til følgende topic:

`teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling`

Sporhund konsumerer dialogmeldinger fra behandler fra følgende topics:

`teamsykefravr.behandler-dialogmelding-status` - for å motta status på sendte dialogmeldinger

`teamsykefravr.melding-fra-behandler` - for å motta dialogmeldinger sendt fra behandler

`teamsykmelding.legeerklaering` - for å motta legeerklæringer sendt fra behandler

## Henvendelser
Sporhund eies og forvaltes av Team SAS (Saksbehandling av Sykepenger). Vi kan nås på slack #sykepenger-værsågod
