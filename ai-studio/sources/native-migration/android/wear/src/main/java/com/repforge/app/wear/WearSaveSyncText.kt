package com.repforge.app.wear

import java.util.Locale

private fun syncText(hu:String,en:String,de:String,ro:String,language:String=Locale.getDefault().language)=when(language){
    "en"->en;"de"->de;"ro"->ro;else->hu
}
internal fun wearSaveTitle()=syncText("Mentés és szinkron","Save and sync","Speichern und Sync","Salvare și sincronizare")
internal fun wearSaveRetry()=syncText("Újrapróbálás","Retry","Erneut versuchen","Reîncearcă")
internal fun wearSaveBack()=syncText("‹ Vissza","‹ Back","‹ Zurück","‹ Înapoi")
internal fun wearSaveEmpty()=syncText("Nincs várakozó művelet","No pending operation","Keine ausstehende Aktion","Nicio acțiune în așteptare")
internal fun wearSaveLabel(phase:WearSavePhase,language:String=Locale.getDefault().language)=when(phase){
    WearSavePhase.SAVING->syncText("Mentés folyamatban","Saving","Wird gespeichert","Se salvează",language)
    WearSavePhase.LOCAL->syncText("Órán mentve","Saved on watch","Auf Uhr gespeichert","Salvat pe ceas",language)
    WearSavePhase.WAITING->syncText("Szinkronra vár","Awaiting sync","Wartet auf Sync","Așteaptă sincronizarea",language)
    WearSavePhase.SYNCED->syncText("Szinkronizálva","Synced","Synchronisiert","Sincronizat",language)
    WearSavePhase.ERROR->syncText("Szinkron ellenőrzése","Check save and sync","Speichern und Sync prüfen","Verifică salvarea",language)
}
internal fun wearSaveDetail(state:WearSaveSyncState?):String=when {
    state==null->wearSaveEmpty()
    state.reason=="local"->syncText("A helyi mentés nem sikerült. Próbáld újra; az adat telefonos mentése még nincs igazolva.","Local saving failed. Retry; phone saving is not confirmed.","Lokales Speichern fehlgeschlagen. Erneut versuchen; auf dem Telefon ist nichts bestätigt.","Salvarea locală a eșuat. Reîncearcă; salvarea pe telefon nu este confirmată.")
    state.reason in setOf("error","superseded","processed")->syncText("A telefon nem igazolta minden művelet mentését. Ellenőrizd az edzést a telefonon.","The phone has not confirmed every operation. Check the workout on the phone.","Das Telefon hat nicht alle Aktionen bestätigt. Training auf dem Telefon prüfen.","Telefonul nu a confirmat toate acțiunile. Verifică antrenamentul pe telefon.")
    state.phase==WearSavePhase.SYNCED->syncText("A telefon tartósan visszaigazolta a műveleteket.","The phone durably confirmed the operations.","Das Telefon hat die Aktionen dauerhaft bestätigt.","Telefonul a confirmat durabil acțiunile.")
    state.phase==WearSavePhase.SAVING->syncText("A helyi mentés igazolására várunk.","Waiting for local saving.","Warten auf lokales Speichern.","Se așteaptă salvarea locală.")
    else->syncText("${state.pending} művelet az órán megőrizve. A telefon visszaigazolására várunk. Újrapróbálható, ha a kapcsolat helyreáll.","${state.pending} operations kept on the watch. Waiting for phone confirmation. Retry when connected.","${state.pending} Aktionen auf der Uhr gespeichert. Warten auf Telefonbestätigung. Nach Verbindung erneut versuchen.","${state.pending} acțiuni păstrate pe ceas. Așteptăm confirmarea telefonului. Reîncearcă după reconectare.")
}
