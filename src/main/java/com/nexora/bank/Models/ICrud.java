package com.nexora.bank.Models;

import java.util.List;

public interface ICrud<T> {

    void add(T t) ;
    void edit(T t) ;
    void remove(T t) ;
    List<T> getAll() ;

}
