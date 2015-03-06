package com.example.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.beans.Tech;

public interface TechRepository extends MongoRepository<Tech, String> {

	public List<Tech> findBytechName(String techName);

}
