package ru.gr0946x.net;

public enum MessageType {
    LOGIN,           // LOGIN:username:password
    REGISTER,        // REGISTER:username:password
    AUTH_OK,         // AUTH_OK:
    AUTH_FAIL,       // AUTH_FAIL:причина

    MSG,             // MSG:от:кому:текст
    BROADCAST,       // BROADCAST:от:текст

    USER_LIST,       // USER_LIST:user1,user2,user3

    HISTORY_REQUEST, // HISTORY_REQUEST:собеседник
    HISTORY_ITEM,    // HISTORY_ITEM:от:кому:текст:дата
    HISTORY_END,     // HISTORY_END:

    SEARCH_REQUEST,  // SEARCH_REQUEST:собеседник:слово
    SEARCH_ITEM,     // SEARCH_ITEM:от:кому:текст:дата
    SEARCH_END,      // SEARCH_END:

    INFO,
    ERROR,
    USER_LIST_REQUEST,
    DISCONNECT
}