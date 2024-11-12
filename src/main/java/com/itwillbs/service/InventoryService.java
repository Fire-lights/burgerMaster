package com.itwillbs.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.itwillbs.domain.inventory.IncomingDTO;
import com.itwillbs.domain.inventory.IncomingInsertDTO;
import com.itwillbs.domain.inventory.IncomingItemsDTO;

import com.itwillbs.domain.inventory.InventoryItemDTO;
import com.itwillbs.entity.Incoming;
import com.itwillbs.entity.IncomingItems;
import com.itwillbs.entity.InventoryItem;
import com.itwillbs.repository.IncomingItemsRepository;
import com.itwillbs.repository.IncomingRepository;
import com.itwillbs.repository.InventoryRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
	private final InventoryRepository inventoryRepository;
	private final IncomingRepository incomingRepository;
	private final IncomingItemsRepository incomingItemsRepository;

	// 재고 전체 조회 (페이지네이션 지원)
	public Page<InventoryItemDTO> getInventoryItems(Pageable pageable) {
		log.info("getInventoryItems()");
		return inventoryRepository.getAllInventoryItems(pageable);
	}

	// 재고 부족 품목만 조회 (검색 조건 포함)
	public Page<InventoryItemDTO> findInventoryItemsByOutOfStock(String itemCodeOrName, String itemType,
			Pageable pageable) {
		log.info("findInventoryItemsByOutOfStock()");
		return inventoryRepository.findInventoryItemsByOutOfStock(itemCodeOrName, itemType, pageable);
	}

	// 재고 검색 (검색 조건과 페이지네이션)
	public Page<InventoryItemDTO> findInventoryItemsBySearch(String itemCodeOrName, String itemType,
			Pageable pageable) {
		log.info("findInventoryItems()");
		return inventoryRepository.findInventoryItems(itemCodeOrName, itemType, pageable);
	}

	// 재고량, 최소필요 재고량 수정(update)
	public void updateInventory(String itemCode, int quantity, int minReqQuantity) throws Exception {
		InventoryItem item = inventoryRepository.findById(itemCode)
				.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 품목 코드입니다."));
		item.setQuantity(quantity);
		item.setMinReqQuantity(minReqQuantity);
		inventoryRepository.save(item);
	}

	// 입고 페이지 진입시 조회
	public Page<IncomingDTO> getIncomingLists(Pageable pageable) {
		log.info("getIncomingLists()");

		// 페이지 사이즈에 맞는 입고 테이블 데이터 조회
		Page<IncomingDTO> incomingByPage = incomingRepository.getIncomingLists(pageable);

		// 각 입고데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
		incomingByPage.forEach(dto -> {

			String incomingId = dto.getIncomingId();

			// incoming_items테이블에서 품목의 이름과 갯수를 구한다.
			List<IncomingItemsDTO> itemNames = incomingItemsRepository.findIncomingItemsListById(incomingId);

			// 품목중 첫번째 품목의 이름을 저장
			dto.setIncomingItemDisplay(itemNames.get(0).getItemName());

			// 품목 갯수 - 1을 저장
			dto.setOtherCount(itemNames.size() - 1);

		});

		return incomingByPage;
	}

	// 입고 목록 검색 (검색 조건과 페이지네이션 포함)
	public Page<IncomingDTO> findIncomingBySearch(String itemCodeOrName, String reasonOfIncoming,
			Timestamp incomingStartDate_start, Timestamp incomingStartDate_end, String incomingId, String prodOrQualId,
			String status, String managerCodeOrName, Pageable pageable) {
		log.info("findIncomingBySearch()");

		// 리포지토리 호출 시 itemCodeOrName 포함
		Page<IncomingDTO> incomingByPage = incomingRepository.findIncomingLists(reasonOfIncoming,
				incomingStartDate_start, incomingStartDate_end, incomingId, prodOrQualId, status, managerCodeOrName,
				itemCodeOrName, pageable);

		// 각 입고 데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
		// ※최적화 생각해야함
		incomingByPage.forEach(dto -> {

			String incomingId2 = dto.getIncomingId();

			// incoming_items 테이블에서 품목코드와 품목이름을 구함
			List<IncomingItemsDTO> itemNames = incomingItemsRepository.findIncomingItemsListById(incomingId2);

			if (!itemNames.isEmpty()) {
				// 첫 번째 품목의 이름을 설정
				dto.setIncomingItemDisplay(itemNames.get(0).getItemName());
				// 나머지 품목 갯수 설정
				dto.setOtherCount(itemNames.size() - 1);
			} else {
				dto.setIncomingItemDisplay("");
				dto.setOtherCount(0);
			}
		});

		return incomingByPage;
	}

	public List<IncomingItemsDTO> getIncomingItems(String incomingId) {

		return incomingItemsRepository.findByIncomingItems(incomingId);
	}

	// 입고 상세 정보 모달창에서 입고 상태 업데이트
	@Transactional
	public void updateIncomingStatus(String incomingId) {
		Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now());
		int updatedRows = incomingRepository.updateIncomingStatus(incomingId, currentTime);
		if (updatedRows == 0) {
			throw new EntityNotFoundException("해당 입고 ID가 존재하지 않습니다: " + incomingId);
		}
	}

	public List<IncomingInsertDTO> findIncomingInsertList() {

		// 생산 완료가되었지만 아직 입고 등록이 안된 생산데이터 저장
		List<IncomingInsertDTO> incomingInsertDTOProd = incomingRepository.findAllEndOfProduction();

		// 각 생산완료된 데이터행마다 품목의 이름과 갯수를 구하기 위한 반복문
		// 생산코드 하나에 품목 하나만 있으므로 구현하지 않음. 필요시 구현

		// 입하 검품완료가되었지만 아직 입고 등록이 안된 검품데이터 저장
		List<IncomingInsertDTO> incomingInsertDTOQual = incomingRepository.findAllEndOfQuality();

		// 각 검품완료된 데이터행마다 품목의 이름과 갯수를 구하기 위한 반복문
		// ※최적화 생각해야함
		incomingInsertDTOQual.forEach(dto -> {

			String QualitySaleId = dto.getIncomingInsertCode();

			// quality_sale_items테이블에서 품목코드와 품목이름을 구함
//            List<IncomingItemsDTO> itemNames = incomingItemsRepository.findQualitySaleItemsById(QualitySaleId);
//
//            if (!itemNames.isEmpty()) {
//                // 첫 번째 품목의 이름을 설정
//                dto.setIncomingItemDisplay(itemNames.get(0).getItemName());
//                // 나머지 품목 갯수 설정
//                dto.setOtherCount(itemNames.size() - 1);
//            } else {
//                dto.setIncomingItemDisplay("");
//                dto.setOtherCount(0);
//            }
		});

		incomingInsertDTOProd.addAll(incomingInsertDTOQual);

		return null;
	}

    @Transactional
    public void updateInventoryItems(List<InventoryItemDTO> InventoryItemDTOList) throws Exception {
        log.info("InventoryService.updateInventoryItems() - InventoryItemDTOList: {}", InventoryItemDTOList);

        for (InventoryItemDTO itemData : InventoryItemDTOList) {
            String itemCode = itemData.getItemCode();
            Integer quantity = itemData.getQuantity();
            Integer minReqQuantity = itemData.getMinReqQuantity();

            InventoryItem item = inventoryRepository.findById(itemCode)
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 품목 코드입니다: " + itemCode));
            item.setQuantity(quantity);
            item.setMinReqQuantity(minReqQuantity);
            inventoryRepository.save(item);
        }
    }

//	// 입고 목록 검색 (검색 조건과 페이지네이션)
//	public Page<IncomingDTO> findIncomingBySearch(
//	        String itemCodeOrName,
//	        String reasonOfIncoming,
//	        Timestamp incomingStartDate_start,
//	        Timestamp incomingStartDate_end,
//	        String incomingId,
//	        String prodOrQualId,
//	        String status,
//	        String managerCodeOrName,
//	        Pageable pageable) {
//	    log.info("findIncomingBySearch()");
//
//	    // 서비스 로직 수행
//	    Page<IncomingDTO> incomingByPage = incomingRepository.findIncomingLists(
//	            reasonOfIncoming,
//	            incomingStartDate_start,
//	            incomingStartDate_end,
//	            incomingId,
//	            prodOrQualId,
//	            status,
//	            managerCodeOrName,
//	            pageable);
//		
//		// 각 입고데이터마다 품목의 이름과 갯수를 구하기 위한 반복문
//		incomingByPage.forEach(dto -> {
//
//			String incomingId2 = dto.getIncomingId();
//
//			// incoming_items테이블에서 품목코드와 품목이름을 구한다.
//			List<IncomingItemsDTO> itemNames = incomingItemsRepository.findIncomingItemsListById(incomingId2);
//
//			// 품목중 첫번째 품목의 이름을 저장
//			dto.setIncomingItemDisplay(itemNames.get(0).getItemName());
//
//			// 품목 갯수 - 1을 저장
//			dto.setOtherCount(itemNames.size() - 1);
//
//		});
//		
//		return incomingByPage;
//	}

}
