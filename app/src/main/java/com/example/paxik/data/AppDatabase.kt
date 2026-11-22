package com.example.paxik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Folder::class, Space::class, Link::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun spaceDao(): SpaceDao
    abstract fun linkDao(): LinkDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `spans` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `links` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`fromId` INTEGER NOT NULL, " +
                            "`toId` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_links_fromId_toId` " +
                            "ON `links` (`fromId`, `toId`)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `links_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`fromType` TEXT NOT NULL, " +
                            "`fromId` INTEGER NOT NULL, " +
                            "`toType` TEXT NOT NULL, " +
                            "`toId` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `links_new` (`id`, `fromType`, `fromId`, `toType`, `toId`) " +
                            "SELECT `id`, 'NOTE', `fromId`, 'NOTE', `toId` FROM `links`"
                )
                db.execSQL("DROP TABLE `links`")
                db.execSQL("ALTER TABLE `links_new` RENAME TO `links`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_links_fromType_fromId_toType_toId` " +
                            "ON `links` (`fromType`, `fromId`, `toType`, `toId`)"
                )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(seedCallback)
                    .build()
                    .also { instance = it }
            }
    }
}