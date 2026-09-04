package com.example.viewobackend.repository

import com.example.viewobackend.model.Campaign
import com.example.viewobackend.model.Media
import com.example.viewobackend.model.Playlist
import com.example.viewobackend.model.Screen
import org.springframework.data.jpa.repository.JpaRepository

interface ScreenRepository : JpaRepository<Screen, String>
interface CampaignRepository : JpaRepository<Campaign, String>
interface MediaRepository : JpaRepository<Media, String>
interface PlaylistRepository : JpaRepository<Playlist, String>
