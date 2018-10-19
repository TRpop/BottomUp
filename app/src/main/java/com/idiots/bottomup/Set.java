package com.idiots.bottomup;

public class Set<T> {

    private Node<T> head, tail;

    public Set(){
        head = new Node<>();
        tail = head;
    }

    public boolean has(T value){
        Node<T> node = head;
        while(node.hasNext()){
            node = node.getNext();
            if(value.equals(node.getValue())){
                return true;
            }
        }
        return false;
    }

    public boolean add(T value){
        if(has(value)){
            return false;
        }else{
            tail.setNext(new Node<>(value));
            tail = tail.getNext();
            return true;
        }
    }

    private class Node<T>{

        private Node<T> next;
        private T value;

        public Node(){
            next = null;
            value = null;
        }

        public Node(Node<T> next, T value){
            setNext(next);
            setValue(value);
        }

        public Node(T value){
            next = null;
            setValue(value);
        }

        public boolean hasNext(){
            return next != null;
        }

        public void setNext(Node<T> next) {
            this.next = next;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public Node<T> getNext() {
            return next;
        }

        public T getValue() {
            return value;
        }
    }
}
