package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item){
        itemRepository.save(item);
    }

    // 변경 감지 기능 사용
    @Transactional
    public Item updateItem(Long itemId, String name, int price, int stockQuantity) {

        Item findItem = itemRepository.findOne(itemId);
        findItem.change(name, price, stockQuantity);

        /* 위와 같이 setter 대신 change 메서드를 Entity 객체에 만들어야 추적이 쉽다.
        findItem.setPrice(param.getPrice());
        findItem.setName(param.getName());
        findItem.setStockQuantity(param.getStockQuantity());
         */
//        itemRepository.save(findItem); // merge 방식
        return findItem;
    }

    public List<Item> findItems(){
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId){
        return itemRepository.findOne(itemId);
    }
}
