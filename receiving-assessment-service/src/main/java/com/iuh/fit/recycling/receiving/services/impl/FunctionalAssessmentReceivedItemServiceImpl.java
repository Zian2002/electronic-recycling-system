package com.iuh.fit.recycling.receiving.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iuh.fit.recycling.receiving.entities.FunctionalAssessmentReceivedItem;
import com.iuh.fit.recycling.receiving.entities.FunctionalAssessmentStatus;
import com.iuh.fit.recycling.receiving.entities.ReceivedItem;
import com.iuh.fit.recycling.receiving.entities.ReceivedStatus;
import com.iuh.fit.recycling.receiving.exception.BadRequestException;
import com.iuh.fit.recycling.receiving.exception.NotFoundException;
import com.iuh.fit.recycling.receiving.models.FunctionalAssessmentItemRequest;
import com.iuh.fit.recycling.receiving.models.ItemStatusDTO;
import com.iuh.fit.recycling.receiving.repositories.FunctionalAssessmentReceivedItemRepository;
import com.iuh.fit.recycling.receiving.services.FunctionalAssessmentReceivedItemService;
import com.iuh.fit.recycling.receiving.services.ReceivedItemService;
import com.iuh.fit.recycling.receiving.services.S3Service;
import com.iuh.fit.recycling.receiving.utils.CheckFileUtil;
import com.iuh.fit.recycling.receiving.utils.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FunctionalAssessmentReceivedItemServiceImpl implements FunctionalAssessmentReceivedItemService {

    private final ReceivedItemService receivedItemService;
    private final FunctionalAssessmentReceivedItemRepository functionalAssessmentReceivedItemRepository;
    private final JmsTemplate template;
    private final S3 s3Service;
    private final ObjectMapper mapper = new ObjectMapper();


    @Override
    public List<FunctionalAssessmentReceivedItem> findAllByStatus(FunctionalAssessmentStatus status) {
        return functionalAssessmentReceivedItemRepository.findAllByStatus(status);
    }

    @Override
    public List<FunctionalAssessmentReceivedItem> findAll() {
        return functionalAssessmentReceivedItemRepository.findAll();
    }

    @Override
    public FunctionalAssessmentReceivedItem findByQuotingItemId(String quotingItemId) {
        return functionalAssessmentReceivedItemRepository.findByReceivedItemQuotingItemId(quotingItemId)
                .orElseThrow(() -> new NotFoundException("quotingItem not found in assessment process"));
    }


    @Override
    public FunctionalAssessmentReceivedItem updateFunctionalAssessmentProof(FunctionalAssessmentItemRequest request, List<MultipartFile> assessmentProof) throws IOException {
        if (request == null || request.getQuotingItemId() == null || request.getAccepted() == null)
            throw new BadRequestException("quotingItemId and accepted is required");

        if (assessmentProof == null || assessmentProof.size() < 4 || !CheckFileUtil.checkFile(assessmentProof))
            throw new BadRequestException("Need 3 image and 1 video to submit");

        FunctionalAssessmentReceivedItem functionalAssessmentReceivedItem = findByQuotingItemId(request.getQuotingItemId());

        if (!functionalAssessmentReceivedItem.getStatus().equals(FunctionalAssessmentStatus.PENDING)){
            throw new BadRequestException("The received item has been " + functionalAssessmentReceivedItem.getStatus());
        }


        List<String> proof = new ArrayList<String>();
        for (MultipartFile multipartFile: assessmentProof){
            String fileUrl = s3Service.uploadFile(multipartFile);
            System.out.println(fileUrl);
            proof.add(fileUrl);
        }

        functionalAssessmentReceivedItem.setFunctionalAssessmentProof(proof);
        functionalAssessmentReceivedItem.setNote(request.getNote());
        functionalAssessmentReceivedItem.setStatus(request.getAccepted() ? FunctionalAssessmentStatus.ACCEPTED : FunctionalAssessmentStatus.DENIED);

        functionalAssessmentReceivedItem = functionalAssessmentReceivedItemRepository.save(functionalAssessmentReceivedItem);

        ItemStatusDTO itemStatusDTO = ItemStatusDTO.builder()
                .quotingItemId(request.getQuotingItemId())
                .accepted(request.getAccepted())
                .time(DateTimeUtil.getDateTimeString(LocalDateTime.now()))
                .build();

        String itemStatusString = mapper.writeValueAsString(itemStatusDTO);

        template.convertAndSend("recycling_assessment", itemStatusString);

        return functionalAssessmentReceivedItem;
    }
}
