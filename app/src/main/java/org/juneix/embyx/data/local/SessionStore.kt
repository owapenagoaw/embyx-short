package com.lalakiop.embyx.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.lalakiop.embyx.core.model.Session

private val Context.dataStore by preferencesDataStore(name = "embyx_session")

class SessionStore(private val context: Context) {

    private object Keys {
        val server = stringPreferencesKey("server")
        val token = stringPreferencesKey("token")
        val userId = stringPreferencesKey("user_id")
        val username = stringPreferencesKey("username")
    }

    val sessionFlow: Flow<Session> = context.dataStore.data.map { prefs: Preferences ->
        Session(
            server = prefs[Keys.server].orEmpty(),
            token = prefs[Keys.token].orEmpty(),
            userId = prefs[Keys.userId].orEmpty(),
            username = prefs[Keys.username].orEmpty()
        )
    }

    suspend fun saveSession(session: Session) {
        context.dataStore.edit { prefs ->
            prefs[Keys.server] = session.server
            prefs[Keys.token] = session.token
            prefs[Keys.userId] = session.userId
            prefs[Keys.username] = session.username
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.server)
            prefs.remove(Keys.token)
            prefs.remove(Keys.userId)
            prefs.remove(Keys.username)
        }
    }
}
