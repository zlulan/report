package com.aventstack.extentreports.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class AbstractStructure<T> implements Serializable {

    private static final long serialVersionUID = -2630417398255980331L;
    
    private transient List<T> list;

    AbstractStructure() {
        list = Collections.synchronizedList(new ArrayList<>());
    }
    
    public void add(T t) {
        list.add(t);
    }

    public T get(int x) {
        return list.get(x);
    }
    
    public T getLast() {
        if (list.size() == 0)
            return null;
        
        return list.get(list.size() - 1);
    }

    public List<T> getAll() {
        return list;
    }
    
    public int size() {
        if (list == null)
            return 0;
        
        return list.size();
    }
    
    public String removeTest(String codeName){
    	StringBuffer result=new StringBuffer();
    	int i=0;
    	if(list!=null){
		result.append("success"+list.size()+":");
    	for(i=0;i<list.size();i++){
    		Test t=(Test) list.get(i);
    		result.append(t.getCodeName()+",");
    		if(codeName.equals(t.getCodeName())){
    			result.append("执行删除");
    			list.remove(t);
    			break;
    		}
    	}
    	}
    	return result.toString();
    }
    
    public TIterator getIterator() { return new TIterator(); }

    // log iterator
    private class TIterator implements Iterator<T> {
        private int index;

        TIterator() {
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return list != null && list.size() >= index + 1;
        }

        @Override
        public T next() {
            if (hasNext()) {
                return list.get(index++);
            }

            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            list.remove(index);
        }
    }
    
}
