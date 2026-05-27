package com.game.backend.controller;

import com.game.backend.dto.PatchNoteResponse;
import com.game.backend.service.PatchNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patches/notes")
@RequiredArgsConstructor
public class PatchNoteController {

    private final PatchNoteService patchNoteService;

    @GetMapping
    public List<PatchNoteResponse> getPatchNotes() {
        return patchNoteService.getPatchNotes();
    }
}