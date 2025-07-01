package com.vincent.hris.master.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.vincent.hris.master.model.Title;

@Repository
public interface TitleRepository extends JpaRepository<Title, Integer> {

}
