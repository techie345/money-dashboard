package com.techie345.moneys.financial.asset;
import java.util.UUID;
import com.techie345.moneys.financial.*;
import jakarta.persistence.*;
@Entity @Table(name="asset") public class AssetEntity extends FinancialEntity {
 @Column(nullable=false,length=200) String name; @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) AssetKind kind; @Column(name="value_cents",nullable=false) long valueCents;
 protected AssetEntity(){} public AssetEntity(UUID o,String n,AssetKind k,long v){if(o==null||n==null||n.isBlank()||k==null||v<0)throw new IllegalArgumentException("invalid asset");ownerId=o;name=n;kind=k;valueCents=v;} public String getName(){return name;} public AssetKind getKind(){return kind;} public long getValueCents(){return valueCents;} public void update(String n,AssetKind k,long v){if(n==null||n.isBlank()||k==null||v<0)throw new IllegalArgumentException("invalid asset");name=n;kind=k;valueCents=v;}
}
