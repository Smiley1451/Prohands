package com.anand.prohands.data.chat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anand.prohands.data.chat.converters.MapConverter

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, ParticipantEntity::class, ConversationParticipantCrossRef::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class, MapConverter::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                ).fallbackToDestructiveMigration().build() // Added fallback migration
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @androidx.room.TypeConverter
    fun toMessageStatus(name: String): MessageStatus = MessageStatus.valueOf(name)

    @androidx.room.TypeConverter
    fun fromMessageType(type: MessageType): String = type.name

    @androidx.room.TypeConverter
    fun toMessageType(name: String): MessageType = MessageType.valueOf(name)
}
