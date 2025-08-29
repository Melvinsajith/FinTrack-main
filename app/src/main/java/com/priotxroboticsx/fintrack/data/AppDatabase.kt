package com.priotxroboticsx.fintrack.data

import android.content.Context
import androidx.room.*

@Database(entities = [Account::class, Transaction::class, User::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "fintrack_db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}
