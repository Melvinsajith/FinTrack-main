package com.priotxroboticsx.fintrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(account: Account)
    @Update suspend fun update(account: Account)
    @Delete suspend fun delete(account: Account)
    @Query("SELECT * FROM accounts ORDER BY name ASC") fun getAllAccounts(): Flow<List<Account>>
    @Query("SELECT * FROM accounts WHERE id = :id") fun getAccount(id: Int): Flow<Account?>
}

@Dao interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(transaction: Transaction)
    @Delete suspend fun delete(transaction: Transaction)
    @Query("SELECT * FROM transactions ORDER BY date DESC") fun getAllTransactions(): Flow<List<Transaction>>
}

@Dao interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(user: User)
    @Query("SELECT * FROM user_profile WHERE id = 1") fun getUser(): Flow<User?>
}