package com.example.paxik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Folder::class, Space::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun spaceDao(): SpaceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`parentId` INTEGER)"
                )
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `folderId` INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `spaces` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL)"
                )
                db.execSQL("INSERT INTO `spaces` (`id`, `name`) VALUES (1, 'Главная')")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `spaceId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `folders` ADD COLUMN `spaceId` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO `spaces` (`id`, `name`) VALUES (1, 'Главная')")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notes.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(seedCallback)
                    .build()
                    .also { instance = it }
            }
    }
}