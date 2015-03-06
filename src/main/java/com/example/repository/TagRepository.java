package com.example.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import com.example.beans.Tags;

@Component
public interface TagRepository extends MongoRepository<Tags, Long> {

	public List<Tags> findBytagName(String tagName);

	public List<Tags> findBytagNameAndMillisec(String tagName, Long millisec);

}
