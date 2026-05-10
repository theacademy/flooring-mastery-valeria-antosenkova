# Flooring Mastery — Pseudocode 
---

## 1. The Main Application Loop


```
START application
  load Spring context (wires all dependencies together)
  get controller from context
  call controller.run()

FUNCTION run():
  running = true
  WHILE running is true:
    display main menu
    choice = read integer from user (must be 1–6)
    IF choice == 1 → displayOrders()
    IF choice == 2 → addOrder()
    IF choice == 3 → editOrder()
    IF choice == 4 → removeOrder()
    IF choice == 5 → exportData()
    IF choice == 6 →
      display goodbye message
      running = false
  END WHILE
END
```

The loop keeps going forever until the user picks 6. Each option is its own method so the `run()` function stays readable.

---

## 2. Display Orders

Read a date, fetch the orders, print them.

```
FUNCTION displayOrders():
  show "Display Orders" banner
  date = ask user for a date (MM-DD-YYYY format)

  TRY:
    orders = service.getOrdersByDate(date)
    display orders in a formatted table
  CATCH NoOrdersOnDateException:
    show error "No orders found for that date"
  CATCH PersistenceException:
    show error "Could not load orders"

  show "Press Enter to continue"
```

The service throws `NoOrdersOnDateException` instead of returning an empty list, because an empty list and "nothing exists for that date" are the same thing — and it's more useful to give the user a clear message than silently show a blank table.

---

## 3. Add Order

Collect input, show a preview, then ask for confirmation before saving.

```
FUNCTION addOrder():
  show "Add Order" banner

  TRY:
    date = ask user for a FUTURE date
          (loop until date is after today)

    load available taxes from file   → to show the user valid states
    load available products from file → to show the user valid products

    customerName = ask user for customer name
    state        = ask user to pick from the list of valid states
    productType  = ask user to pick from the list of valid products
    area         = ask user for area (must be >= 100 sq ft)

    build an Order object with those values
    set the order number (next available number)
    set the order date

    // Preview step — enrich with pricing before showing the user
    look up tax rate for the chosen state → set on order
    look up product prices for the chosen product → set on order
    calculate all costs (material, labor, tax, total) → set on order

    display full order summary to the user

    IF user confirms "y":
      service.addOrder(date, order)
      show success message
    ELSE:
      show "Order was not saved"

  CATCH DataValidationException:
    show the validation error message
  CATCH PersistenceException:
    show "Could not save order"

  show "Press Enter to continue"
```


---

## 4. Edit Order

Edit is similar to Add but it starts by looking up an existing order and then pre-fills every prompt with the current value.

```
FUNCTION editOrder():
  show "Edit Order" banner

  TRY:
    date        = ask user for the date of the order
    orderNumber = ask user for the order number

    existing = service.getOrder(date, orderNumber)
             // throws OrderNotFoundException if not found

    load available taxes
    load available products

    // Each prompt shows the current value in parentheses.
    // If user just presses Enter, the current value is kept.
    customerName = ask for name       (default: existing.customerName)
    state        = ask for state      (default: existing.state)
    productType  = ask for product    (default: existing.productType)
    area         = ask for area       (default: existing.area)

    build updated Order with those values (same order number)

    // Same preview step as addOrder
    look up tax rate for state → set on order
    look up product prices → set on order
    calculate costs → set on order

    display full order summary

    IF user confirms "y":
      service.editOrder(date, updated)
      show success message
    ELSE:
      show "Changes were not saved"

  CATCH OrderNotFoundException:
    show "Order not found"
  CATCH DataValidationException:
    show the validation error message
  CATCH PersistenceException:
    show "Could not edit order"

  show "Press Enter to continue"
```


---

## 5. Remove Order

Remove follows the same lookup-then-confirm pattern.

```
FUNCTION removeOrder():
  show "Remove Order" banner

  TRY:
    date        = ask user for date
    orderNumber = ask user for order number

    order = service.getOrder(date, orderNumber)
          // throws OrderNotFoundException if not found

    display order summary so user can see what they're removing

    IF user confirms "y":
      service.removeOrder(date, orderNumber)
      show success message
    ELSE:
      show "Order was not removed"

  CATCH OrderNotFoundException:
    show "Order not found"
  CATCH PersistenceException:
    show "Could not remove order"

  show "Press Enter to continue"
```

---

## 6. Export All Data


```
FUNCTION exportData():
  show "Export" banner

  TRY:
    service.exportAllData()
    show "Exported to Backup/DataExport.txt"
  CATCH PersistenceException:
    show "Export failed"

  show "Press Enter to continue"
```

---

## 7. Service Layer — Validation Logic

Before any order is saved or updated, the service checks every field.

```
FUNCTION validateDate(date):
  IF date is NOT after today:
    THROW DataValidationException("Order date must be in the future")

FUNCTION validateOrder(order):
  IF customerName is blank:
    THROW DataValidationException("Name may not be blank")

  IF customerName contains characters outside [a-zA-Z0-9 .,]:
    THROW DataValidationException("Name contains invalid characters")

  IF state is blank:
    THROW DataValidationException("State may not be blank")

  IF productType is blank:
    THROW DataValidationException("Product type may not be blank")

  IF area is null OR area < 100:
    THROW DataValidationException("Area must be at least 100 sq ft")
```


---

## 8. Service Layer — Enriching an Order with Tax and Product Data

Both `addOrder` and `editOrder` need to fill in the tax rate and product prices before calculating costs. Rather than repeat that block twice, I pulled it into a helper.

```
FUNCTION enrichOrderFromTaxAndProduct(order):
  tax = taxDao.getTaxByState(order.state)
  IF tax is null:
    THROW DataValidationException("State not available for orders")

  product = productDao.getProductByType(order.productType)
  IF product is null:
    THROW DataValidationException("Product type not available")

  order.taxRate                = tax.taxRate
  order.costPerSquareFoot      = product.costPerSquareFoot
  order.laborCostPerSquareFoot = product.laborCostPerSquareFoot
```


---

## 9. Service Layer — Cost Calculation


```
FUNCTION calculateOrderCosts(order):
  materialCost = order.area × order.costPerSquareFoot
               → round to 2 decimal places (HALF_UP)

  laborCost = order.area × order.laborCostPerSquareFoot
            → round to 2 decimal places (HALF_UP)

  tax = (materialCost + laborCost) × order.taxRate ÷ 100
      → round to 2 decimal places (HALF_UP)

  total = materialCost + laborCost + tax

  set all four values on the order object
  return the order
```

**Example with real numbers (Order #1, Ada Lovelace):**
```
area = 249 sq ft, product = Tile, cost/sqft = $3.50, labor/sqft = $4.15, tax = 25%

materialCost = 249 × 3.50  = $871.50
laborCost    = 249 × 4.15  = $1,033.35
tax          = (871.50 + 1033.35) × 0.25 = $476.21
total        = 871.50 + 1033.35 + 476.21 = $2,381.06
```

---

## 10. Service Layer — Getting the Next Order Number

Every new order needs a unique number. The rule is: find the highest existing order number across all dates and add 1.

```
FUNCTION getNextOrderNumber():
  allOrders = orderDao.getAllOrders()
            // returns Map<Date, List<Order>>

  IF allOrders is empty:
    RETURN 1

  max = 0
  FOR EACH date in allOrders:
    FOR EACH order in orders for that date:
      IF order.orderNumber > max:
        max = order.orderNumber

  RETURN max + 1
```


---

## 11. DAO Layer — Reading Order Files

Each date's orders live in a file named `Orders_MMDDYYYY.txt`. The read process is:

```
FUNCTION getOrdersByDate(date):
  filename = "Orders_" + date.format("MMddyyyy") + ".txt"
  file = new File(ordersDirectory + "/" + filename)

  IF file does not exist:
    RETURN empty list   // not an error — just no orders that day

  RETURN loadOrdersFromFile(file, date)

FUNCTION loadOrdersFromFile(file, date):
  orders = empty list
  open file for reading
  skip first line  // that's the header row

  FOR EACH remaining line in file:
    IF line is not blank:
      order = unmarshalOrder(line, date)
      add order to list

  RETURN orders
```

---

## 12. DAO Layer — Writing Order Files

Every add, edit, and remove does a full rewrite of the file.

```
FUNCTION writeOrdersToFile(date, orders):
  create the orders directory if it doesn't exist
  filename = build filename from date
  open file for writing (overwrites anything existing)

  write the header line

  FOR EACH order in orders:
    write marshalOrder(order)  // converts order to a comma-delimited string

  close file
```


---

## 13. DAO Layer — Parsing Orders with Commas in the Name

This was the trickiest part of the file I/O. A customer name like "Acme, Inc." contains a comma, so a naive split by comma gives the wrong result.

```
FUNCTION unmarshalOrder(line, date):
  tokens = line.split(",")
  // Problem: "1,Acme, Inc.,CA,25.00,..." splits into more tokens than expected

  // The key insight: there are exactly 10 fixed fields AFTER the name:
  // State, TaxRate, ProductType, Area, CostPerSqFt, LaborCostPerSqFt,
  // MaterialCost, LaborCost, Tax, Total
  //
  // So: token[0] = OrderNumber
  //     token[1] through token[len-10-1] = CustomerName (may span multiple tokens)
  //     token[len-10] onward = the 10 fixed fields

  fixedFieldsAfterName = 10
  nameEndIndex = tokens.length - fixedFieldsAfterName

  orderNumber  = parse tokens[0] as integer
  customerName = join tokens[1] through tokens[nameEndIndex - 1] with ","
  state        = tokens[nameEndIndex]
  taxRate      = tokens[nameEndIndex + 1]
  ... and so on for the remaining 8 fields

  RETURN new Order with all fields set
```

No matter how many commas are in the name, the 10 fields after it are always exactly 10. Counting from the right side of the token array is stable. "Acme, Inc." becomes two tokens between index 1 and `nameEndIndex - 1`, and they get joined back together with a comma.

---

## 14. DAO Layer — Removing an Order

```
FUNCTION removeOrder(date, orderNumber):
  orders = getOrdersByDate(date)
  removed = orders.removeIf(o → o.orderNumber == orderNumber)

  IF removed is false:
    THROW PersistenceException("Order not found")

  IF orders is now empty:
    delete the file entirely  // don't leave empty files around
  ELSE:
    writeOrdersToFile(date, orders)
```


---

## 15. DAO Layer — Export All Data

```
FUNCTION exportAllData(exportFilePath):
  allOrders = getAllOrders()
            // TreeMap → already sorted by date

  create any missing parent directories for the export file
  open export file for writing

  write export header (same as regular header + "OrderDate" column)

  FOR EACH date in allOrders (in chronological order because TreeMap):
    dateString = date.format("MM-dd-yyyy")
    FOR EACH order on that date:
      write marshalOrder(order) + "," + dateString

  close file
```

---

## 16. How the View Handles Input — The "Press Enter to Keep" Pattern

This pattern is used in all four edit-flow prompts (name, state, product, area).

```
FUNCTION getCustomerNameInput(currentName):
  IF currentName is null:
    prompt = "Enter customer name: "
  ELSE:
    prompt = "Enter customer name (currentName): "

  LOOP forever:
    input = read string from user

    IF input is blank AND currentName is not null:
      RETURN currentName   // user just pressed Enter → keep existing

    IF input is not blank AND matches valid character pattern:
      RETURN input

    IF input is blank:
      show error "Name may not be blank"
    ELSE:
      show error "Name contains invalid characters"
```

Same logic applies to state, product, and area — each one knows whether it's in "add mode" (currentValue is null, blank is an error) or "edit mode" (currentValue is set, blank means keep it).

---

## 17. How Exceptions Flow Through the Layers


```
User types something bad
  → View catches it immediately (e.g. non-numeric input) and re-prompts

User picks a state not in the file
  → DAO returns null
  → Service sees null, throws DataValidationException
  → Controller catches it, shows error message to user

File is missing or unreadable
  → DAO throws PersistenceException
  → Service may re-throw it, or wrap it in a more specific exception
  → Controller catches it, shows error message to user

User asks to edit order #99 that doesn't exist
  → DAO returns empty list for that date
  → Service finds nothing in the list, throws OrderNotFoundException
  → Controller catches it, shows "Order not found"
```

